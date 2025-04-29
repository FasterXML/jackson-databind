/**
Contains extended support for "external" packages: things that
may or may not be present in runtime environment, but that are
commonly enough used so that explicit support can be added.
<p>
Currently included extensions are:
<ul>
 <li>Java core XML datatypes: the reason these are
considered "external" is that some platforms that claim to be conformant
are only partially so (Google Android, GAE) and do not included these
 types; and with Java 9 and above also due to JPMS reasons.
  </li>
 <li>Selected {@code java.sql} types.
  </li>
 <li>Selected {@code java.beans} annotations: {@code @Transient}, {@code ConstructorProperties}.
  </li>
 <li>Java (8) Time (JSR-310) type support: as of Jackson 3.0 included in databind
  but added similar to {@code JacksonModule}s for improved configurability.
  </li>
</ul>
*/

package tools.jackson.databind.ext;
