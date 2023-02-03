package com.fasterxml.jackson.databind.jsontype.ext;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

@SuppressWarnings("hiding")
public class ExternalTypeCustomResolverTest extends BaseMapTest
{
    // [databind#1288]
    public static class ClassesWithoutBuilder {

        public static class CreditCardDetails implements PaymentDetails {

            protected String cardHolderFirstName;
            protected String cardHolderLastName;
            protected String number;
            protected String expiryDate;
            protected int csc;
            protected String address;
            protected String zipCode;
            protected String city;
            protected String province;

            protected String countryCode;

            protected String description;

            public void setCardHolderFirstName(String cardHolderFirstName) {
                this.cardHolderFirstName = cardHolderFirstName;
            }

            public void setCardHolderLastName(String cardHolderLastName) {
                this.cardHolderLastName = cardHolderLastName;
            }

            public void setNumber(String number) {
                this.number = number;
            }

            public void setExpiryDate(String expiryDate) {
                this.expiryDate = expiryDate;
            }

            public void setCsc(int csc) {
                this.csc = csc;
            }

            public void setAddress(String address) {
                this.address = address;
            }

            public void setZipCode(String zipCode) {
                this.zipCode = zipCode;
            }

            public void setCity(String city) {
                this.city = city;
            }

            public void setProvince(String province) {
                this.province = province;
            }

            public void setCountryCode(String countryCode) {
                this.countryCode = countryCode;
            }

            public void setDescription(String description) {
                this.description = description;
            }


        }

        public static class EncryptedCreditCardDetails implements PaymentDetails {

            protected UUID paymentInstrumentID;

            protected String name;

            public void setPaymentInstrumentID(UUID paymentInstrumentID) {
                this.paymentInstrumentID = paymentInstrumentID;
            }

            public void setName (String name) {
                this.name = name;
            }

        }

        public enum FormOfPayment {
            INDIVIDUAL_CREDIT_CARD (CreditCardDetails.class), COMPANY_CREDIT_CARD (
                    CreditCardDetails.class), INSTRUMENTED_CREDIT_CARD (EncryptedCreditCardDetails.class);

            private final Class<? extends PaymentDetails> clazz;

            FormOfPayment(final Class<? extends PaymentDetails> clazz) {
                this.clazz = clazz;
            }

            @SuppressWarnings("unchecked")
            public <T extends PaymentDetails> Class<T> getDetailsClass () {
                return (Class<T>) this.clazz;
            }

            public static FormOfPayment fromDetailsClass(Class<PaymentDetails> detailsClass) {
                for (FormOfPayment fop : FormOfPayment.values ()) {
                    if (fop.clazz == detailsClass) {
                        return fop;
                    }
                }
                throw new IllegalArgumentException("not found");
            }
        }

        public interface PaymentDetails {
            public interface Builder {
                PaymentDetails build();
            }
        }

        public static class PaymentMean {

            FormOfPayment formOfPayment;

            PaymentDetails paymentDetails;

            public void setFormOfPayment(FormOfPayment formOfPayment) {
                this.formOfPayment = formOfPayment;
            }

            @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "form_of_payment", visible = true)
            @JsonTypeIdResolver(PaymentDetailsTypeIdResolver.class)
            public void setPaymentDetails(PaymentDetails paymentDetails) {
                this.paymentDetails = paymentDetails;
            }
        }

        public static class PaymentDetailsTypeIdResolver extends TypeIdResolverBase {
            @SuppressWarnings("unchecked")
            @Override
            public String idFromValue (Object value) {
                if (! (value instanceof PaymentDetails)) {
                    return null;
                }
                return FormOfPayment.fromDetailsClass ((Class<PaymentDetails>) value.getClass ()).name ();
            }

            @Override
            public String idFromValueAndType (Object value, Class<?> suggestedType) {
                return this.idFromValue (value);
            }

            @Override
            public JavaType typeFromId (DatabindContext context, String id) {
                return context.getTypeFactory().constructType(FormOfPayment.valueOf(id).getDetailsClass ());
            }

            @Override
            public String getDescForKnownTypeIds () {
                return "PaymentDetails";
            }

            @Override
            public Id getMechanism () {
                return JsonTypeInfo.Id.CUSTOM;
            }
        }
    }

    public static class ClassesWithBuilder {

        @JsonDeserialize (builder = CreditCardDetails.IndividualCreditCardDetailsBuilder.class)
        public static class CreditCardDetails implements PaymentDetails {
            @JsonPOJOBuilder(withPrefix = "")
            public static class CompanyCreditCardDetailsBuilder implements Builder {
                private String cardHolderFirstName;
                private String cardHolderLastName;
                private String number;
                private int csc;

                @Override
                public CreditCardDetails build() {
                    return new CreditCardDetails (cardHolderFirstName, cardHolderLastName, number, csc,
                            "COMPANY CREDIT CARD");
                }

                public CompanyCreditCardDetailsBuilder cardHolderFirstName(final String cardHolderFirstName) {
                    this.cardHolderFirstName = cardHolderFirstName;
                    return this;
                }

                public CompanyCreditCardDetailsBuilder cardHolderLastName(final String cardHolderLastName) {
                    this.cardHolderLastName = cardHolderLastName;
                    return this;
                }

                public CompanyCreditCardDetailsBuilder csc(final int csc) {
                    this.csc = csc;
                    return this;
                }

                public CompanyCreditCardDetailsBuilder number(final String number) {
                    this.number = number;
                    return this;
                }
            }

            @JsonPOJOBuilder (withPrefix = "")
            public static class IndividualCreditCardDetailsBuilder implements Builder {
                private String cardHolderFirstName;
                private String cardHolderLastName;
                private String number;
                private int    csc;
                private String description;

                @Override
                public CreditCardDetails build () {
                    return new CreditCardDetails(cardHolderFirstName, cardHolderLastName, number, csc,
                            description);
                }

                public IndividualCreditCardDetailsBuilder cardHolderFirstName(final String cardHolderFirstName) {
                    this.cardHolderFirstName = cardHolderFirstName;
                    return this;
                }

                public IndividualCreditCardDetailsBuilder cardHolderLastName(final String cardHolderLastName) {
                    this.cardHolderLastName = cardHolderLastName;
                    return this;
                }

                public IndividualCreditCardDetailsBuilder csc (final int csc) {
                    this.csc = csc;
                    return this;
                }

                public IndividualCreditCardDetailsBuilder description (final String description) {
                    this.description = description;
                    return this;
                }

                public IndividualCreditCardDetailsBuilder number (final String number) {
                    this.number = number;
                    return this;
                }
            }

            protected final String cardHolderFirstName;
            protected final String cardHolderLastName;
            protected final String number;
            protected final int    csc;

            protected final String description;

            public CreditCardDetails (final String cardHolderFirstName, final String cardHolderLastName,
                    final String number, final int csc,
                    final String description) {
                super();
                this.cardHolderFirstName = cardHolderFirstName;
                this.cardHolderLastName = cardHolderLastName;
                this.number = number;
                this.csc = csc;
                this.description = description;
            }
        }

        @JsonDeserialize (builder = EncryptedCreditCardDetails.InstrumentedCreditCardBuilder.class)
        public static class EncryptedCreditCardDetails implements PaymentDetails {
            @JsonPOJOBuilder (withPrefix = "")
            public static class InstrumentedCreditCardBuilder implements Builder {
                private UUID   paymentInstrumentID;
                private String name;

                @Override
                public EncryptedCreditCardDetails build () {
                    return new EncryptedCreditCardDetails (this.paymentInstrumentID, this.name);
                }

                public InstrumentedCreditCardBuilder name (final String name) {
                    this.name = name;
                    return this;
                }

                public InstrumentedCreditCardBuilder paymentInstrumentID (final UUID paymentInstrumentID) {
                    this.paymentInstrumentID = paymentInstrumentID;
                    return this;
                }
            }

            protected final UUID paymentInstrumentID;
            protected final String name;

            EncryptedCreditCardDetails (final UUID paymentInstrumentID, final String name) {
                super();
                this.paymentInstrumentID = paymentInstrumentID;
                this.name = name;
            }
        }

        public enum FormOfPayment {
            INDIVIDUAL_CREDIT_CARD (CreditCardDetails.IndividualCreditCardDetailsBuilder.class), COMPANY_CREDIT_CARD (
                    CreditCardDetails.CompanyCreditCardDetailsBuilder.class), INSTRUMENTED_CREDIT_CARD (EncryptedCreditCardDetails.InstrumentedCreditCardBuilder.class);

            private final Class<? extends PaymentDetails.Builder> builderClass;

            FormOfPayment(final Class<? extends PaymentDetails.Builder> builderClass) {
                this.builderClass = builderClass;
            }

            @SuppressWarnings ("unchecked")
            public <T extends PaymentDetails> Class<T> getDetailsClass() {
                return (Class<T>) this.builderClass.getEnclosingClass();
            }

            public static FormOfPayment fromDetailsClass(Class<PaymentDetails> detailsClass) {
                for (FormOfPayment fop : FormOfPayment.values()) {
                    if (fop.builderClass.getEnclosingClass() == detailsClass) {
                        return fop;
                    }
                }
                throw new IllegalArgumentException("not found");
            }
        }

        public interface PaymentDetails {
            public interface Builder {
                PaymentDetails build();
            }
        }

        @JsonDeserialize(builder = PaymentMean.Builder.class)
        public static class PaymentMean {
            @JsonPOJOBuilder(withPrefix = "")
            @JsonPropertyOrder({ "form_of_payment", "payment_details" })
            public static class Builder {
                private FormOfPayment  formOfPayment;
                private PaymentDetails paymentDetails;

                public PaymentMean build () {
                    return new PaymentMean(this.formOfPayment, this.paymentDetails);
                }

                // if you annotate with @JsonIgnore, it works, but the value
                // disappears in the constructor
                public Builder formOfPayment (final FormOfPayment val) {
                    this.formOfPayment = val;
                    return this;
                }

                @JsonTypeInfo (use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "form_of_payment", visible = true)
                @JsonTypeIdResolver (PaymentDetailsTypeIdResolver.class)
                public Builder paymentDetails (final PaymentDetails val) {
                    this.paymentDetails = val;
                    return this;
                }
            }

            public static Builder create() {
                return new Builder();
            }

            protected final FormOfPayment  formOfPayment;
            protected final PaymentDetails paymentDetails;

            PaymentMean (final FormOfPayment formOfPayment, final PaymentDetails paymentDetails) {
                super ();
                this.formOfPayment = formOfPayment;
                this.paymentDetails = paymentDetails;
            }
        }

        public static class PaymentDetailsTypeIdResolver extends TypeIdResolverBase {
            @SuppressWarnings ("unchecked")
            @Override
            public String idFromValue (Object value) {
                if (! (value instanceof PaymentDetails)) {
                    return null;
                }
                return FormOfPayment.fromDetailsClass ((Class<PaymentDetails>) value.getClass ()).name ();
            }

            @Override
            public String idFromValueAndType (Object value, Class<?> suggestedType) {
                return this.idFromValue (value);
            }

            @Override
            public JavaType typeFromId(DatabindContext context, String id) {
                return context.getTypeFactory().constructType(FormOfPayment.valueOf (id).getDetailsClass ());
            }

            @Override
            public String getDescForKnownTypeIds() {
                return "PaymentDetails";
            }

            @Override
            public Id getMechanism() {
                return JsonTypeInfo.Id.CUSTOM;
            }
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    // [databind#1288]
    public void testExternalWithCustomResolver() throws Exception
    {
        // given
        final String asJson1 = a2q(
"{'form_of_payment':'INDIVIDUAL_CREDIT_CARD', 'payment_details':{'card_holder_first_name':'John',\n"
+"'card_holder_last_name':'Doe',  'number':'XXXXXXXXXXXXXXXX', 'expiry_date':'MM/YY',\n"
+ "'csc':666,'address':'10 boulevard de Sebastopol','zip_code':'75001','city':'Paris',\n"
+"'province':'Ile-de-France','country_code':'FR','description':'John Doe personal credit card'}}"
        );
        ClassesWithoutBuilder.PaymentMean ob1 = MAPPER.readValue(asJson1, ClassesWithoutBuilder.PaymentMean.class);
        assertNotNull(ob1);
    }

    // [databind#1288]
    public void testExternalWithCustomResolverAndBuilder() throws Exception
    {
        final String asJson2 = a2q(
"{'form_of_payment':'INSTRUMENTED_CREDIT_CARD',\n"
+"'payment_details':{\n"
+"'payment_instrument_id':'00000000-0000-0000-0000-000000000000',\n"
+" 'name':'Mr John Doe encrypted credit card'}}"
        );

        ClassesWithBuilder.PaymentMean ob2 = MAPPER.readValue(asJson2, ClassesWithBuilder.PaymentMean.class);
        assertNotNull(ob2);
    }
}
