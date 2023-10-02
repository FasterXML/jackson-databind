package com.fasterxml.jackson.databind;

/**
 * Sample class from Jackson tutorial ("JacksonInFiveMinutes")
 */
public class JacksonInFiveMinutesTutorialTest extends BaseMapTest
{

    public static class FiveMinuteUser {
        public enum Gender { MALE, FEMALE };

        public static class Name
        {
            private String _first, _last;

            public Name() { }
            public Name(String f, String l) {
                _first = f;
                _last = l;
            }

            public String getFirst() { return _first; }
            public String getLast() { return _last; }

            public void setFirst(String s) { _first = s; }
            public void setLast(String s) { _last = s; }

            @Override
            public boolean equals(Object o)
            {
                if (o == this) return true;
                if (o == null || o.getClass() != getClass()) return false;
                Name other = (Name) o;
                return _first.equals(other._first) && _last.equals(other._last);
            }
        }

        private Gender _gender;
        private Name _name;
        private boolean _isVerified;
        private byte[] _userImage;

        public FiveMinuteUser() { }

        public FiveMinuteUser(String first, String last, boolean verified, Gender g, byte[] data)
        {
            _name = new Name(first, last);
            _isVerified = verified;
            _gender = g;
            _userImage = data;
        }

        public Name getName() { return _name; }
        public boolean isVerified() { return _isVerified; }
        public Gender getGender() { return _gender; }
        public byte[] getUserImage() { return _userImage; }

        public void setName(Name n) { _name = n; }
        public void setVerified(boolean b) { _isVerified = b; }
        public void setGender(Gender g) { _gender = g; }
        public void setUserImage(byte[] b) { _userImage = b; }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) return true;
            if (o == null || o.getClass() != getClass()) return false;
            FiveMinuteUser other = (FiveMinuteUser) o;
            if (_isVerified != other._isVerified) return false;
            if (_gender != other._gender) return false;
            if (!_name.equals(other._name)) return false;
            byte[] otherImage = other._userImage;
            if (otherImage.length != _userImage.length) return false;
            for (int i = 0, len = _userImage.length; i < len; ++i) {
                if (_userImage[i] != otherImage[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    static class NonMergeConfig {
        public AB loc = new AB(1, 2);
    }

    static class AB {
        public int a;
        public int b;

        protected AB() { }
        public AB(int a0, int b0) {
            a = a0;
            b = b0;
        }
    }

    public void testBeanMergingViaGlobal() throws Exception
    {
        // but with type-overrides
        ObjectMapper mapper = newJsonMapper().setDefaultMergeable(true);
        NonMergeConfig config = mapper.readValue(a2q("{'loc':{'a':3}}"), NonMergeConfig.class);
        assertEquals(3, config.loc.a);
        assertEquals(2, config.loc.b); // original, merged

        // also, test with bigger POJO type; just as smoke test
        FiveMinuteUser user0 = new FiveMinuteUser("Bob", "Bush", true, FiveMinuteUser.Gender.MALE,
                new byte[] { 1, 2, 3, 4, 5 });
        FiveMinuteUser user = mapper.readerFor(FiveMinuteUser.class)
                .withValueToUpdate(user0)
                .readValue(a2q("{'name':{'last':'Brown'}}"));
        assertEquals("Bob", user.getName().getFirst());
        assertEquals("Brown", user.getName().getLast());
    }
}
