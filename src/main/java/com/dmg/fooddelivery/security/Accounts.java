package com.dmg.fooddelivery.security;

import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.common.Database;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Accounts implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final Database database;
    private final PasswordEncoder encoder;

    public Accounts(JdbcTemplate jdbc, Database database, PasswordEncoder encoder) {
        this.jdbc = jdbc; this.database = database; this.encoder = encoder;
    }

    @Override public UserDetails loadUserByUsername(String username) {
        return jdbc.query("SELECT * FROM app_users WHERE username = ?", (rs, row) ->
                new AccountPrincipal(new Actor(rs.getLong("id"), rs.getString("username"),
                        Role.valueOf(rs.getString("role"))), rs.getString("password_hash")), username)
                .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    public Actor current() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw ApiException.forbidden();
        }
        return principal.actor();
    }

    public Actor find(long id) {
        return jdbc.query("SELECT id, username, role FROM app_users WHERE id = ?", (rs, row) ->
                new Actor(rs.getLong("id"), rs.getString("username"), Role.valueOf(rs.getString("role"))), id)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("User"));
    }

    /** Serializes idempotent placement for a customer, including the first use of a key. */
    public void lockCustomer(long id) {
        jdbc.queryForObject("SELECT id FROM app_users WHERE id = ? FOR UPDATE", Long.class, id);
    }

    @Transactional
    public Actor create(String username, String password, Role role) {
        long id = database.insert("INSERT INTO app_users(username, password_hash, role) VALUES (?, ?, ?)",
                username, encoder.encode(password), role.name());
        return new Actor(id, username, role);
    }
}
