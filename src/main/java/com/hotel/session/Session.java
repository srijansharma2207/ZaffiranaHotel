package com.hotel.session;

import com.hotel.model.AppUser;

public final class Session {
    private static AppUser currentUser;

    private Session() {}

    public static AppUser getCurrentUser() { return currentUser; }

    public static void setCurrentUser(AppUser user) { currentUser = user; }

    public static void clear() { currentUser = null; }

    public static boolean isLoggedIn() { return currentUser != null; }

    public static boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }
}
