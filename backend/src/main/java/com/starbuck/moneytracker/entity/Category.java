package com.starbuck.moneytracker.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.starbuck.moneytracker.entity.enum_entites.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enum_entites.LangEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LangEnum lang;

    @ManyToOne(optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(nullable = false)
    @ColumnDefault("0")
    private GeneralStatusEnum status = GeneralStatusEnum.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "category")
    private List<TransactionDetailCategory> transactionDetailLinks;

    /**
     * A mindenki számára elérhető kategóriák nyelvi kulcsként vannak az
     * adatbázisban, ezeknél nincs user beállítva
     * 
     * @return
     */
    public boolean isDefaultCategory() {
        return this.user == null;
    }

    // Autogenerált

    public Category() {
    }

    public Category(String name, User user, LangEnum lang) {
        this.name = name;
        this.user = user;
        this.lang = lang;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GeneralStatusEnum getStatus() {
        return status;
    }

    public void setStatus(GeneralStatusEnum status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LangEnum getLang() {
        return lang;
    }

    public void setLang(LangEnum lang) {
        this.lang = lang;
    }

}
