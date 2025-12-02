package course.examples.nt118.model;

public class UserTokenResponse {
    private String message;
    private UserToken user;

    public String getMessage() {
        return message;
    }

    public UserToken getUser() {
        return user;
    }

    public static class UserToken {
        private String id;
        private String email;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }
    }
}
