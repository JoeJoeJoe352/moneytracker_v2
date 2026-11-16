package com.starbuck.moneytracker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Egy átvezetés (TRANSFER) két oldalát köti össze: a forrás tárcából kimenő és a cél tárcába bejövő
 * tranzakciót.
 */
@Entity
@Table(name = "transfer_transaction_sides")
public class TransferTransactionSide {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** A forrás tárca tranzakciója (kiadás oldal) */
    @OneToOne(optional = false)
    @JoinColumn(name = "src_transaction_id", nullable = false, unique = true)
    private Transaction srcTransaction;

    /** A cél tárca tranzakciója (bevétel oldal) */
    @OneToOne(optional = false)
    @JoinColumn(name = "dst_transaction_id", nullable = false, unique = true)
    private Transaction dstTransaction;

    public TransferTransactionSide() {}

    public TransferTransactionSide(Transaction srcTransaction, Transaction dstTransaction) {
        this.srcTransaction = srcTransaction;
        this.dstTransaction = dstTransaction;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getSrcTransaction() {
        return srcTransaction;
    }

    public void setSrcTransaction(Transaction srcTransaction) {
        this.srcTransaction = srcTransaction;
    }

    public Transaction getDstTransaction() {
        return dstTransaction;
    }

    public void setDstTransaction(Transaction dstTransaction) {
        this.dstTransaction = dstTransaction;
    }
}
