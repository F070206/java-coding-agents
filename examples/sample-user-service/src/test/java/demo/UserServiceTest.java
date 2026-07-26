package demo;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class UserServiceTest { private final UserService service = new UserService(); @Test void acceptsValidPhoneCode() { assertTrue(service.loginByPhone("13800138000", "246810")); } @Test void rejectsWrongCode() { assertFalse(service.loginByPhone("13800138000", "000000")); } }
