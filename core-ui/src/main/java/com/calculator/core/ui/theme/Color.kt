package com.calculator.core.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Pure Monochromatic Apple/Bang & Olufsen Palette (Titanium Minimalist) ───
val Background        = Color(0xFF08090C)  // Deepest matte obsidian carbon
val SurfaceCard       = Color(0xFF12141C)  // Dark slate card surface
val SurfaceElevated   = Color(0xFF1A1D27)  // Lighter container surface
val SurfaceBorder     = Color(0xFF262936)  // Subtle 1px boundary stroke

// ─── Monochromatic Accents ───────────────────────────────────────────────────
val AccentPrimary     = Color(0xFFFFFFFF)  // Pure titanium white
val AccentSecondary   = Color(0xFF94A3B8)  // Crisp slate gray
val AccentMuted       = Color(0xFF64748B)  // Subdued secondary text

// ─── Calculator Keyboard Tokens ──────────────────────────────────────────────
val KeyNumeric        = Color(0xFF12141C)  // Dark slate capsule for digits
val KeyOperator       = Color(0xFF1A1D27)  // Slightly lighter slate for operators
val KeyEqual          = Color(0xFFFFFFFF)  // Solid titanium white equal button
val KeyClear          = Color(0xFF261416)  // Subtle dark crimson for AC
val KeySpecial        = Color(0xFF1A1D27)  // Slate 800 for ( ) %

// ─── Text Tokens ─────────────────────────────────────────────────────────────
val TextPrimary       = Color(0xFFF8FAFC)  // Crisp white
val TextSecondary     = Color(0xFF94A3B8)  // Muted slate gray
val TextMuted         = Color(0xFF64748B)  // Placeholder gray

// ─── Semantic Tokens ─────────────────────────────────────────────────────────
val ErrorRed          = Color(0xFFEF4444)
val SuccessGreen      = Color(0xFF10B981)

// ─── Legacy aliases for backward compatibility ────────────────────────────────
val AccentViolet      = Color(0xFFFFFFFF)
val AccentVioletLight = Color(0xFF94A3B8)
val AccentCyan        = Color(0xFFFFFFFF)
val AccentCyanLight   = Color(0xFFF8FAFC)
val AccentAmber       = Color(0xFFE2E8F0)
val AccentAmberLight  = Color(0xFFCBD5E1)
val ObsidianBackground = Background
val ObsidianCard       = SurfaceCard
val NeonCyan           = AccentPrimary
val ElectricViolet     = AccentPrimary
val CoralPink          = ErrorRed
val KeyDarkGray        = KeyNumeric
val KeyLightGray       = KeySpecial
val KeyOrange          = AccentPrimary
val TextPlaceholder    = TextMuted
