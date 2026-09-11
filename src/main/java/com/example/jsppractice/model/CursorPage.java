package com.example.jsppractice.model;

import java.util.List;

public record CursorPage<T>(List<T> content, boolean hasNext, boolean hasPrev) {
}
