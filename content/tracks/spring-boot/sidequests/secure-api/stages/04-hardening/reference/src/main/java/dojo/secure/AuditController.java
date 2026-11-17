package dojo.secure;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    private final Audit audit;

    public AuditController(Audit audit) {
        this.audit = audit;
    }

    @GetMapping("/api/admin/audit")
    public List<Audit.EventView> audit(@RequestParam(required = false) String username) {
        return audit.latest(username);
    }
}
