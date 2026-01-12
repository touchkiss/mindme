<template>
  <div class="cyber-app">
    <TargetCursor />
    <!-- Scanlines overlay -->
    <div class="scanlines"></div>
    
    <!-- Sidebar -->
    <aside class="cyber-sidebar">
      <div class="sidebar-logo">
        <div class="logo-glow"></div>
        <span class="logo-icon">🧠</span>
        <span class="logo-text">MINDME</span>
        <span class="logo-version">v2.0</span>
      </div>
      
      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" v-slot="{ isActive }">
          <span class="nav-indicator" :class="{ active: isActive }"></span>
          <span class="nav-icon">◈</span>
          <span class="nav-label">Dashboard</span>
        </router-link>
        <router-link to="/activities" class="nav-item">
          <span class="nav-indicator"></span>
          <span class="nav-icon">◉</span>
          <span class="nav-label">Activities</span>
        </router-link>
        <router-link to="/knowledge" class="nav-item">
          <span class="nav-indicator"></span>
          <span class="nav-icon">◐</span>
          <span class="nav-label">Knowledge</span>
        </router-link>
        <router-link to="/reports" class="nav-item">
          <span class="nav-indicator"></span>
          <span class="nav-icon">◎</span>
          <span class="nav-label">Reports</span>
        </router-link>
        <router-link to="/sites" class="nav-item">
          <span class="nav-indicator"></span>
          <span class="nav-icon">◑</span>
          <span class="nav-label">Sites</span>
        </router-link>
      </nav>
      
      <div class="sidebar-footer">
        <div class="system-status">
          <span class="status-dot"></span>
          <span>SYSTEM ONLINE</span>
        </div>
      </div>
    </aside>
    
    <!-- Main Content -->
    <main class="cyber-main">
      <div class="main-grid"></div>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import TargetCursor from './components/TargetCursor.vue'
</script>
