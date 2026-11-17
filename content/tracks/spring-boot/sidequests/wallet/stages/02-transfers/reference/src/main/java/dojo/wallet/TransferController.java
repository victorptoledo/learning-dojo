package dojo.wallet;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping
    public ResponseEntity<TransferView> transfer(@Valid @RequestBody NewTransfer request, UriComponentsBuilder uri) {
        Transfer transfer = transfers.transfer(request);
        return ResponseEntity.created(uri.path("/transfers/{id}").build(transfer.getId()))
                .body(TransferView.of(transfer));
    }

    @GetMapping("/{id}")
    public TransferView get(@PathVariable long id) {
        return TransferView.of(transfers.get(id));
    }
}
