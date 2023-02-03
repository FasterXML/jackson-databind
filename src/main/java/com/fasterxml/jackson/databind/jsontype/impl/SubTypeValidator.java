package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Helper class used to encapsulate rules that determine subtypes that
 * are invalid to use, even with default typing, mostly due to security
 * concerns.
 * Used by <code>BeanDeserializerFactory</code>
 *
 * @since 2.8.11
 */
public class SubTypeValidator
{
    protected final static String PREFIX_SPRING = "org.springframework.";

    protected final static String PREFIX_C3P0 = "com.mchange.v2.c3p0.";

    /**
     * Set of well-known "nasty classes", deserialization of which is considered dangerous
     * and should (and is) prevented by default.
     */
    protected final static Set<String> DEFAULT_NO_DESER_CLASS_NAMES;
    static {
        Set<String> s = new HashSet<String>();
        // Courtesy of [https://github.com/kantega/notsoserial]:
        // (and wrt [databind#1599])
        s.add("org.apache.commons.collections.functors.InvokerTransformer");
        s.add("org.apache.commons.collections.functors.InstantiateTransformer");
        s.add("org.apache.commons.collections4.functors.InvokerTransformer");
        s.add("org.apache.commons.collections4.functors.InstantiateTransformer");
        s.add("org.codehaus.groovy.runtime.ConvertedClosure");
        s.add("org.codehaus.groovy.runtime.MethodClosure");
        s.add("org.springframework.beans.factory.ObjectFactory");
        s.add("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl");
        s.add("org.apache.xalan.xsltc.trax.TemplatesImpl");
        // [databind#1680]: may or may not be problem, take no chance
        s.add("com.sun.rowset.JdbcRowSetImpl");
        // [databind#1737]; JDK provided
        s.add("java.util.logging.FileHandler");
        s.add("java.rmi.server.UnicastRemoteObject");
        // [databind#1737]; 3rd party
//s.add("org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor"); // deprecated by [databind#1855]
        s.add("org.springframework.beans.factory.config.PropertyPathFactoryBean");
        // [databind#2680]
        s.add("org.springframework.aop.config.MethodLocatingFactoryBean");
        s.add("org.springframework.beans.factory.config.BeanReferenceFactoryBean");

// s.add("com.mchange.v2.c3p0.JndiRefForwardingDataSource"); // deprecated by [databind#1931]
// s.add("com.mchange.v2.c3p0.WrapperConnectionPoolDataSource"); // - "" -
        // [databind#1855]: more 3rd party
        s.add("org.apache.tomcat.dbcp.dbcp2.BasicDataSource");
        s.add("com.sun.org.apache.bcel.internal.util.ClassLoader");
        // [databind#1899]: more 3rd party
        s.add("org.hibernate.jmx.StatisticsService");
        s.add("org.apache.ibatis.datasource.jndi.JndiDataSourceFactory");
        // [databind#2032]: more 3rd party; data exfiltration via xml parsed ext entities
        s.add("org.apache.ibatis.parsing.XPathParser");

        // [databind#2052]: Jodd-db, with jndi/ldap lookup
        s.add("jodd.db.connection.DataSourceConnectionProvider");

        // [databind#2058]: Oracle JDBC driver, with jndi/ldap lookup
        s.add("oracle.jdbc.connector.OracleManagedConnectionFactory");
        s.add("oracle.jdbc.rowset.OracleJDBCRowSet");

        // [databind#2097]: some 3rd party, one JDK-bundled
        s.add("org.slf4j.ext.EventData");
        s.add("flex.messaging.util.concurrent.AsynchBeansWorkManagerExecutor");
        s.add("com.sun.deploy.security.ruleset.DRSHelper");
        s.add("org.apache.axis2.jaxws.spi.handler.HandlerResolverImpl");

        // [databind#2186], [databind#2670]: yet more 3rd party gadgets
        s.add("org.jboss.util.propertyeditor.DocumentEditor");
        s.add("org.apache.openjpa.ee.RegistryManagedRuntime");
        s.add("org.apache.openjpa.ee.JNDIManagedRuntime");
        s.add("org.apache.openjpa.ee.WASRegistryManagedRuntime"); // [#2670] addition
        s.add("org.apache.axis2.transport.jms.JMSOutTransportInfo");

        // [databind#2326] (2.9.9)
        s.add("com.mysql.cj.jdbc.admin.MiniAdmin");

        // [databind#2334]: logback-core (2.9.9.1)
        s.add("ch.qos.logback.core.db.DriverManagerConnectionSource");

        // [databind#2341]: jdom/jdom2 (2.9.9.1)
        s.add("org.jdom.transform.XSLTransformer");
        s.add("org.jdom2.transform.XSLTransformer");

        // [databind#2387], [databind#2460]: EHCache
        s.add("net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup");
        s.add("net.sf.ehcache.hibernate.EhcacheJtaTransactionManagerLookup");

        // [databind#2389]: logback/jndi
        s.add("ch.qos.logback.core.db.JNDIConnectionSource");

        // [databind#2410]: HikariCP/metricRegistry config
        s.add("com.zaxxer.hikari.HikariConfig");
        // [databind#2449]: and sub-class thereof
        s.add("com.zaxxer.hikari.HikariDataSource");

        // [databind#2420]: CXF/JAX-RS provider/XSLT
        s.add("org.apache.cxf.jaxrs.provider.XSLTJaxbProvider");

        // [databind#2462]: commons-configuration / -2
        s.add("org.apache.commons.configuration.JNDIConfiguration");
        s.add("org.apache.commons.configuration2.JNDIConfiguration");

        // [databind#2469]: xalan
        s.add("org.apache.xalan.lib.sql.JNDIConnectionPool");
        // [databind#2704]: xalan2
        s.add("com.sun.org.apache.xalan.internal.lib.sql.JNDIConnectionPool");

        // [databind#2478]: commons-dbcp 1.x, p6spy
        // [databind#3004]: commons-dbcp 1.x
        s.add("org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS");
        s.add("org.apache.commons.dbcp.datasources.PerUserPoolDataSource");
        s.add("org.apache.commons.dbcp.datasources.SharedPoolDataSource");

        s.add("com.p6spy.engine.spy.P6DataSource");

        // [databind#2498]: log4j-extras (1.2)
        s.add("org.apache.log4j.receivers.db.DriverManagerConnectionSource");
        s.add("org.apache.log4j.receivers.db.JNDIConnectionSource");

        // [databind#2526]: some more ehcache
        s.add("net.sf.ehcache.transaction.manager.selector.GenericJndiSelector");
        s.add("net.sf.ehcache.transaction.manager.selector.GlassfishSelector");

        // [databind#2620]: xbean-reflect
        s.add("org.apache.xbean.propertyeditor.JndiConverter");

        // [databind#2631]: shaded hikari-config
        s.add("org.apache.hadoop.shaded.com.zaxxer.hikari.HikariConfig");

        // [databind#2634]: ibatis-sqlmap, anteros-core/-dbcp
        s.add("com.ibatis.sqlmap.engine.transaction.jta.JtaTransactionConfig");
        s.add("br.com.anteros.dbcp.AnterosDBCPConfig");
        // [databind#2814]: anteros-dbcp
        s.add("br.com.anteros.dbcp.AnterosDBCPDataSource");

        // [databind#2642][databind#2854]: javax.swing (jdk)
        s.add("javax.swing.JEditorPane");
        s.add("javax.swing.JTextPane");

        // [databind#2648], [databind#2653]: shire-core
        s.add("org.apache.shiro.realm.jndi.JndiRealmFactory");
        s.add("org.apache.shiro.jndi.JndiObjectFactory");

        // [databind#2658]: ignite-jta (, quartz-core)
        s.add("org.apache.ignite.cache.jta.jndi.CacheJndiTmLookup");
        s.add("org.apache.ignite.cache.jta.jndi.CacheJndiTmFactory");
        s.add("org.quartz.utils.JNDIConnectionProvider");

        // [databind#2659]: aries.transaction.jms
        s.add("org.apache.aries.transaction.jms.internal.XaPooledConnectionFactory");
        s.add("org.apache.aries.transaction.jms.RecoverablePooledConnectionFactory");

        // [databind#2660]: caucho-quercus
        s.add("com.caucho.config.types.ResourceRef");

        // [databind#2662]: aoju/bus-proxy
        s.add("org.aoju.bus.proxy.provider.RmiProvider");
        s.add("org.aoju.bus.proxy.provider.remoting.RmiProvider");

        // [databind#2664]: activemq-core, activemq-pool, activemq-pool-jms

        s.add("org.apache.activemq.ActiveMQConnectionFactory"); // core
        s.add("org.apache.activemq.ActiveMQXAConnectionFactory");
        s.add("org.apache.activemq.spring.ActiveMQConnectionFactory");
        s.add("org.apache.activemq.spring.ActiveMQXAConnectionFactory");
        s.add("org.apache.activemq.pool.JcaPooledConnectionFactory"); // pool
        s.add("org.apache.activemq.pool.PooledConnectionFactory");
        s.add("org.apache.activemq.pool.XaPooledConnectionFactory");
        s.add("org.apache.activemq.jms.pool.XaPooledConnectionFactory"); // pool-jms
        s.add("org.apache.activemq.jms.pool.JcaPooledConnectionFactory");

        // [databind#2666]: apache/commons-jms
        s.add("org.apache.commons.proxy.provider.remoting.RmiProvider");

        // [databind#2682]: commons-jelly
        s.add("org.apache.commons.jelly.impl.Embedded");

        // [databind#2688], [databind#3004]: apache/drill
        s.add("oadd.org.apache.xalan.lib.sql.JNDIConnectionPool");
        s.add("oadd.org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS");
        s.add("oadd.org.apache.commons.dbcp.datasources.PerUserPoolDataSource");
        s.add("oadd.org.apache.commons.dbcp.datasources.SharedPoolDataSource");

        // [databind#2698]: weblogic w/ oracle/aq-jms
        // (note: dependency not available via Maven Central, but as part of
        // weblogic installation, possibly fairly old version(s))
        s.add("oracle.jms.AQjmsQueueConnectionFactory");
        s.add("oracle.jms.AQjmsXATopicConnectionFactory");
        s.add("oracle.jms.AQjmsTopicConnectionFactory");
        s.add("oracle.jms.AQjmsXAQueueConnectionFactory");
        s.add("oracle.jms.AQjmsXAConnectionFactory");

        // [databind#2764]: org.jsecurity:
        s.add("org.jsecurity.realm.jndi.JndiRealmFactory");

        // [databind#2798]: com.pastdev.httpcomponents:
        s.add("com.pastdev.httpcomponents.configuration.JndiConfiguration");

        // [databind#2826], [databind#2827]
        s.add("com.nqadmin.rowset.JdbcRowSetImpl");
        s.add("org.arrah.framework.rdbms.UpdatableJdbcRowsetImpl");

        // [databind#2986], [databind#3004]: dbcp2
        s.add("org.apache.commons.dbcp2.datasources.PerUserPoolDataSource");
        s.add("org.apache.commons.dbcp2.datasources.SharedPoolDataSource");
        s.add("org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS");

        // [databind#2996]: newrelic-agent + embedded-logback-core
        // (derivative of #2334 and #2389)
        s.add("com.newrelic.agent.deps.ch.qos.logback.core.db.JNDIConnectionSource");
        s.add("com.newrelic.agent.deps.ch.qos.logback.core.db.DriverManagerConnectionSource");

        // [databind#2997]/[databind#3004]: tomcat/naming-factory-dbcp (embedded dbcp 1.x)
        // (derivative of #2478)
        s.add("org.apache.tomcat.dbcp.dbcp.cpdsadapter.DriverAdapterCPDS");
        s.add("org.apache.tomcat.dbcp.dbcp.datasources.PerUserPoolDataSource");
        s.add("org.apache.tomcat.dbcp.dbcp.datasources.SharedPoolDataSource");

        // [databind#2998]/[databind#3004]: org.apache.tomcat/tomcat-dbcp (embedded dbcp 2.x)
        // (derivative of #2478)
        s.add("org.apache.tomcat.dbcp.dbcp2.cpdsadapter.DriverAdapterCPDS");
        s.add("org.apache.tomcat.dbcp.dbcp2.datasources.PerUserPoolDataSource");
        s.add("org.apache.tomcat.dbcp.dbcp2.datasources.SharedPoolDataSource");

        // [databind#2999]: org.glassfish.web/javax.servlet.jsp.jstl (embedded Xalan)
        // (derivative of #2469)
        s.add("com.oracle.wls.shaded.org.apache.xalan.lib.sql.JNDIConnectionPool");

        // [databind#3003]: another case of embedded Xalan (derivative of #2469)
        s.add("org.docx4j.org.apache.xalan.lib.sql.JNDIConnectionPool");

        DEFAULT_NO_DESER_CLASS_NAMES = Collections.unmodifiableSet(s);
    }

    /**
     * Set of class names of types that are never to be deserialized.
     */
    protected Set<String> _cfgIllegalClassNames = DEFAULT_NO_DESER_CLASS_NAMES;

    private final static SubTypeValidator instance = new SubTypeValidator();

    protected SubTypeValidator() { }

    public static SubTypeValidator instance() { return instance; }

    public void validateSubType(DeserializationContext ctxt, JavaType type,
            BeanDescription beanDesc) throws JsonMappingException
    {
        // There are certain nasty classes that could cause problems, mostly
        // via default typing -- catch them here.
        final Class<?> raw = type.getRawClass();
        String full = raw.getName();

        main_check:
        do {
            if (_cfgIllegalClassNames.contains(full)) {
                break;
            }

            // 18-Dec-2017, tatu: As per [databind#1855], need bit more sophisticated handling
            //    for some Spring framework types
            // 05-Jan-2017, tatu: ... also, only applies to classes, not interfaces
            if (raw.isInterface()) {
                ;
            } else if (full.startsWith(PREFIX_SPRING)) {
                for (Class<?> cls = raw; (cls != null) && (cls != Object.class); cls = cls.getSuperclass()){
                    String name = cls.getSimpleName();
                    // looking for "AbstractBeanFactoryPointcutAdvisor" but no point to allow any is there?
                    if ("AbstractPointcutAdvisor".equals(name)
                            // ditto  for "FileSystemXmlApplicationContext": block all ApplicationContexts
                            || "AbstractApplicationContext".equals(name)) {
                        break main_check;
                    }
                }
            } else if (full.startsWith(PREFIX_C3P0)) {
                // [databind#1737]; more 3rd party
                // s.add("com.mchange.v2.c3p0.JndiRefForwardingDataSource");
                // s.add("com.mchange.v2.c3p0.WrapperConnectionPoolDataSource");
                // [databind#1931]; more 3rd party
                // com.mchange.v2.c3p0.ComboPooledDataSource
                // com.mchange.v2.c3p0.debug.AfterCloseLoggingComboPooledDataSource
                if (full.endsWith("DataSource")) {
                    break main_check;
                }
            }
            return;
        } while (false);

        ctxt.reportBadTypeDefinition(beanDesc,
                "Illegal type (%s) to deserialize: prevented for security reasons", full);
    }
}
