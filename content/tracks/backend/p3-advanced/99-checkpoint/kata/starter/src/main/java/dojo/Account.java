package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A bank account. Only the nickname is the client's to change; the rest belongs to the bank. */
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who owns the account (the X-User the gateway sends). */
    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false, length = 3)
    private String currency;

    private long balanceCents;

    private long overdraftLimitCents;

    protected Account() {}

    public Account(String owner, String nickname, String currency, long balanceCents, long overdraftLimitCents) {
        this.owner = owner;
        this.nickname = nickname;
        this.currency = currency;
        this.balanceCents = balanceCents;
        this.overdraftLimitCents = overdraftLimitCents;
    }

    public void rename(String nickname) {
        this.nickname = nickname;
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getNickname() {
        return nickname;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public long getOverdraftLimitCents() {
        return overdraftLimitCents;
    }
}
