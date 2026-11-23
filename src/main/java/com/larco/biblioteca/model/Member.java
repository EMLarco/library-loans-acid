package com.larco.biblioteca.model;

import java.time.LocalDate;

public record Member(Integer memberId, String name, String contactInfo, String membershipId, LocalDate joinDate) { }
