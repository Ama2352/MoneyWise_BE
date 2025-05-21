package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.CreateWalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.UpdateWalletDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.wallet.WalletDTO;
import JavaProject.MoneyManagement_BE_SE330.services.WalletService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/Wallets")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wallet")
public class WalletController {
    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<List<WalletDTO>> getAllWallets() {
        List<WalletDTO> wallets = walletService.getAllWallets();
        return ResponseEntity.ok(wallets);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletDTO> getWalletById(@PathVariable("walletId") UUID walletId) {
        WalletDTO found = walletService.getWalletById(walletId);
        return ResponseEntity.ok(found);
    }

    @DeleteMapping("/{walletId}")
    public ResponseEntity<UUID> deleteWallet(@PathVariable("walletId") UUID walletId) {
        UUID deletedId = walletService.deleteWalletById(walletId);
        return ResponseEntity.ok(deletedId);
    }

    @PostMapping
    public ResponseEntity<WalletDTO> createWallet(@RequestBody @Valid CreateWalletDTO model) {
        WalletDTO created = walletService.createWallet(model);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<WalletDTO> updateWallet(@RequestBody @Valid UpdateWalletDTO model) {
        WalletDTO updated = walletService.updateWallet(model);
        return ResponseEntity.ok(updated);
    }
}
