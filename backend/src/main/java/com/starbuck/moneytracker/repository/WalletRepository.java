package com.starbuck.moneytracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.starbuck.moneytracker.dto.WalletListResponseDto;
import com.starbuck.moneytracker.dto.WalletSummaryDto;
import com.starbuck.moneytracker.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Walletet lekérdez a megadott id alapján
     */
    @Query("SELECT w FROM Wallet w WHERE w.id=?1 AND w.user.id = ?2 AND w.status = 0")
    Optional<Wallet> getWalletById(long walletId, long userId);

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

    @Query("""
                SELECT new com.starbuck.moneytracker.dto.WalletListResponseDto(
                    w.id,
                    w.name,
                    w.currencyCode,
                    w.type,
                    COALESCE(SUM(t.priceSum), 0)
                )
                FROM Wallet w
                LEFT JOIN w.transactions t
                WHERE w.user.id = ?1 AND w.status = 0
                GROUP BY w.id
                ORDER BY w.id ASC
            """)
    List<WalletListResponseDto> listWalletsWithSumByUserId(long userId);

    /**
     * Visszatér a user összes pénzével, walletenként
     * Lehet null, hogyha még nincs neki tranzakciója adott walleten
     */
    @Query("""
                SELECT new com.starbuck.moneytracker.dto.WalletSummaryDto(
                    w.currencyCode,
                    COALESCE(SUM(t.priceSum), 0)
                )
                FROM Wallet w
                LEFT JOIN w.transactions t
                WHERE w.user.id = ?1 AND w.status = 0
                GROUP BY w.id
                ORDER BY w.id
            """)
    List<WalletSummaryDto> summarizeTotalMoneyForUser(long userId);
}
