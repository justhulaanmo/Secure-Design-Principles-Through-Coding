package sarong_systemsecurity.security;
import sarong_systemsecurity.models.User;

public class PermissionManager {

    public boolean canView(User user) {
        return true; // all roles can view
    }

    public boolean canEdit(User user) {
        return user.getRole().equals("teacher") || user.getRole().equals("admin");
    }

    public boolean canDelete(User user) {
        return user.getRole().equals("admin");
    }
}
