package com.starbuck.moneytracker.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.starbuck.moneytracker.dto.WalletListRowResponseDto;
import com.starbuck.moneytracker.dto.WalletSummaryResponseDto;
import com.starbuck.moneytracker.entity.Wallet;

import jakarta.persistence.LockModeType;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Walletet lekérdez a megadott id alapján
     */
    @Query("SELECT w FROM Wallet w WHERE w.id=?1 AND w.user.id = ?2 AND w.status = 0")
    Optional<Wallet> getWalletById(long walletId, long userId);

    /**
     * Mint a getWalletById, de a tranzakció végéig zárolja a wallet sorát
     * így két párhuzamos művelet nem futhat egyszerre ugyanazon a walleten
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id=?1 AND w.user.id = ?2 AND w.status = 0")
    Optional<Wallet> getWalletByIdForUpdate(long walletId, long userId);

    /**
     * A user walletjeinek lekérdezése
     */
    @Query("""
                SELECT w
                FROM Wallet w
                WHERE w.user.id = ?1 AND w.status = 0
                ORDER BY w.id ASC
            """)
    List<Wallet> findByUserId(long userId);

    /**
     * Walleteket lekérdezi, kiírja hozzá az összeget, az utolsó szinkronizálás
     * időpontját és a létrehozás időpontját (a user szerinti napot a service számolja).
     * A sync időpont subquery-ből jön, mert egy második JOIN megsokszorozná a
     * tranzakció sorokat, és elrontaná a SUM-ot.
     */
    @Query("""
                SELECT new com.starbuck.moneytracker.dto.WalletListRowResponseDto(
                    w.id,
                    w.name,
                    w.currencyCode,
                    w.type,
                    COALESCE(SUM(t.priceSum), 0),
                    (SELECT MAX(bs.syncedAt) FROM BalanceSync bs WHERE bs.wallet = w),
                    w.createdAt
                )
                FROM Wallet w
                LEFT JOIN w.transactions t ON t.status = 0
                WHERE w.user.id = ?1 AND w.status = 0
                GROUP BY w.id
                ORDER BY w.id ASC
            """)
    List<WalletListRowResponseDto> listWalletsWithSumByUserId(long userId);

    /**
     * Adott walletről lekérdezi, hogy mekkora a balance rajta
     */
    @Query("""
                SELECT COALESCE(SUM(t.priceSum), 0)
                FROM Wallet w
                LEFT JOIN w.transactions t ON t.status = 0
                WHERE w.user.id = ?2 AND w.status = 0 AND w.id = ?1
            """)
    BigDecimal getBalanceOfWallet(long walletId, long userId);

    /**
     * Visszatér a user összes pénzével, walletenként
     * Lehet null, hogyha még nincs neki tranzakciója adott walleten
     */
    @Query("""
                SELECT new com.starbuck.moneytracker.dto.WalletSummaryResponseDto(
                    w.currencyCode,
                    COALESCE(SUM(t.priceSum), 0)
                )
                FROM Wallet w
                LEFT JOIN w.transactions t ON t.status = 0
                WHERE w.user.id = ?1 AND w.status = 0
                GROUP BY w.id
                ORDER BY w.id
            """)
    List<WalletSummaryResponseDto> summarizeTotalMoneyForUser(long userId);
}
