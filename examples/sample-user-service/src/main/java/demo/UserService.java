package demo;
public class UserService { public boolean loginByPhone(String phone, String code) { return phone != null && phone.matches("1\\d{10}") && "246810".equals(code); } }
