<template>
  <div id="app">
    <!-- Login Page -->
    <Login v-if="!userStore.isLoggedIn" @login-success="handleLoginSuccess" />
    
    <!-- Main App -->
    <el-container v-else>
      <el-header class="app-header">
        <div class="header-content">
          <div class="logo" @click="goToDashboard">
            <h1>监控中心</h1>
          </div>
          <el-menu 
            :default-active="activeTab" 
            mode="horizontal" 
            @select="handleTabChange"
            class="app-menu"
          >
            <!-- 监控指标 - 独立菜单 -->
            <el-menu-item v-if="hasMenu('metrics')" index="metrics">
              <el-icon><TrendCharts /></el-icon>
              <span>监控指标</span>
            </el-menu-item>
            
            <!-- 告警中心 -->
            <el-sub-menu v-if="hasAnyMenu(['alerts', 'emergency', 'third-party'])" index="alert-center">
              <template #title>
                <el-icon><Bell /></el-icon>
                <span>告警中心</span>
              </template>
              <el-menu-item v-if="hasMenu('alerts')" index="alerts">
                <el-icon><Warning /></el-icon>
                <span>告警规则管理</span>
              </el-menu-item>
              <el-menu-item v-if="hasMenu('emergency')" index="emergency">
                <el-icon><Reading /></el-icon>
                <span>应急知识库</span>
              </el-menu-item>
              <el-menu-item v-if="hasMenu('third-party')" index="third-party">
                <el-icon><Connection /></el-icon>
                <span>第三方告警</span>
              </el-menu-item>
            </el-sub-menu>
            
            <!-- 系统管理 -->
            <el-sub-menu v-if="hasAnyMenu(['users', 'roles', 'agents', 'metric-definitions', 'apps'])" index="system">
              <template #title>
                <el-icon><Tools /></el-icon>
                <span>系统管理</span>
              </template>
              <el-menu-item v-if="hasMenu('agents')" index="agents">
                <el-icon><Cpu /></el-icon>
                <span>代理管理</span>
              </el-menu-item>
              <el-menu-item v-if="hasMenu('metric-definitions')" index="metric-definitions">
                <el-icon><Setting /></el-icon>
                <span>指标定义</span>
              </el-menu-item>
              <el-menu-item v-if="hasMenu('apps')" index="apps">
                <el-icon><Grid /></el-icon>
                <span>应用管理</span>
              </el-menu-item>
              <el-menu-item v-if="hasMenu('users')" index="users">
                <el-icon><User /></el-icon>
                <span>用户管理</span>
              </el-menu-item>
              <el-menu-item v-if="hasMenu('roles')" index="roles">
                <el-icon><UserFilled /></el-icon>
                <span>角色管理</span>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
          <div class="user-info">
            <el-dropdown @command="handleCommand">
              <span class="user-dropdown">
                <el-icon><Avatar /></el-icon>
                {{ userStore.nickname || userStore.username }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <div class="user-dropdown-info">
                    <div class="info-item">
                      <span class="info-label">角色：</span>
                      <span class="info-value">
                        {{ userStore.roles.map(r => r.roleName).join('、') || '无' }}
                      </span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">应用：</span>
                      <span class="info-value">
                        {{ userStore.apps.map(a => a.appName).join('、') || '无' }}
                      </span>
                    </div>
                  </div>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      
      <el-main class="app-main">
        <MonitoringDashboard v-if="activeTab === 'dashboard'" />
        <AgentManagement v-if="activeTab === 'agents'" />
        <MetricCollection v-if="activeTab === 'metrics'" />
        <MetricDefinitions v-if="activeTab === 'metric-definitions'" />
        <AlertManagement v-if="activeTab === 'alerts'" />
        <EmergencyKnowledge v-if="activeTab === 'emergency'" />
        <ThirdPartyAlerts v-if="activeTab === 'third-party'" />
        <UserManagement v-if="activeTab === 'users'" />
        <RoleManagement v-if="activeTab === 'roles'" />
        <AppManagement v-if="activeTab === 'apps'" />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { 
  User, ArrowDown, SwitchButton, Monitor, DataBoard, TrendCharts, 
  Bell, Reading, Setting, Connection, UserFilled, Tools, Warning,
  Cpu, Avatar, Grid
} from '@element-plus/icons-vue'
import { useUserStore } from './stores/user'
import Login from './components/Login.vue'
import AgentManagement from './components/AgentManagement.vue'
import MetricCollection from './components/MetricCollection.vue'
import MetricDefinitions from './components/MetricDefinitions.vue'
import AlertManagement from './components/AlertManagement.vue'
import MonitoringDashboard from './components/MonitoringDashboard.vue'
import EmergencyKnowledge from './components/EmergencyKnowledge.vue'
import ThirdPartyAlerts from './components/ThirdPartyAlerts.vue'
import UserManagement from './components/UserManagement.vue'
import RoleManagement from './components/RoleManagement.vue'
import AppManagement from './components/AppManagement.vue'

export default {
  name: 'App',
  components: {
    User,
    ArrowDown,
    SwitchButton,
    Monitor,
    DataBoard,
    TrendCharts,
    Bell,
    Reading,
    Setting,
    Connection,
    UserFilled,
    Tools,
    Warning,
    Cpu,
    Avatar,
    Grid,
    Login,
    MonitoringDashboard,
    AgentManagement,
    MetricCollection,
    MetricDefinitions,
    AlertManagement,
    EmergencyKnowledge,
    ThirdPartyAlerts,
    UserManagement,
    RoleManagement,
    AppManagement
  },
  setup() {
    const userStore = useUserStore()
    const activeTab = ref('dashboard')
    
    // Restore user from storage on mount
    onMounted(() => {
      userStore.restoreFromStorage()
    })
    
    // Check if user has specific menu permission
    const hasMenu = (menuCode) => {
      return userStore.menus.some(m => m.menuCode === menuCode)
    }
    
    // Check if user has any of the specified menus
    const hasAnyMenu = (menuCodes) => {
      return menuCodes.some(code => hasMenu(code))
    }
    
    const handleTabChange = (key) => {
      activeTab.value = key
    }
    
    const handleLoginSuccess = () => {
      // Set default active tab to dashboard or first available menu
      if (hasMenu('dashboard')) {
        activeTab.value = 'dashboard'
      } else if (userStore.menus.length > 0) {
        activeTab.value = userStore.menus[0].menuCode
      }
    }
    
    const handleCommand = (command) => {
      if (command === 'logout') {
        userStore.logout()
        activeTab.value = 'dashboard'
      }
    }
    
    const goToDashboard = () => {
      if (hasMenu('dashboard')) {
        activeTab.value = 'dashboard'
      }
    }
    
    return {
      userStore,
      activeTab,
      hasMenu,
      hasAnyMenu,
      handleTabChange,
      handleLoginSuccess,
      handleCommand,
      goToDashboard
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  background: #f5f7fa;
  min-height: 100vh;
}

#app {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #000000;
  min-height: 100vh;
}

.app-header {
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  padding: 0 !important;
  height: auto !important;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 0;
  margin-right: 40px;
  cursor: pointer;
}

.logo:hover h1 {
  opacity: 0.8;
}

.logo h1 {
  color: #1976d2;
  font-size: 20px;
  margin: 0;
  font-weight: 600;
}

.app-menu {
  flex: 1;
  border-bottom: none;
  background: transparent;
}

.app-menu .el-menu-item,
.app-menu .el-sub-menu__title {
  font-weight: 500;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}

.user-dropdown:hover {
  color: #1976d2;
}

.app-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 80px);
}

/* Override Element Plus menu styles */
.el-menu--horizontal .el-menu-item:not(.is-disabled):hover,
.el-menu--horizontal .el-menu-item:not(.is-disabled):focus,
.el-menu--horizontal .el-sub-menu__title:hover {
  background-color: #ecf5ff;
}

.el-menu--horizontal .el-menu-item.is-active {
  border-bottom-color: #1976d2;
  color: #1976d2;
}

.el-menu--horizontal > .el-sub-menu.is-active .el-sub-menu__title {
  border-bottom-color: #1976d2;
  color: #1976d2;
}

/* Submenu popup styles */
.el-menu--horizontal .el-menu .el-menu-item {
  height: 40px;
  line-height: 40px;
}

.el-menu--horizontal .el-menu .el-menu-item .el-icon {
  margin-right: 8px;
}

.user-dropdown-info {
  padding: 12px 16px;
  min-width: 180px;
}

.user-dropdown-info .info-item {
  display: flex;
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.user-dropdown-info .info-item:last-child {
  margin-bottom: 0;
}

.user-dropdown-info .info-label {
  color: #909399;
  flex-shrink: 0;
}

.user-dropdown-info .info-value {
  color: #303133;
  word-break: break-all;
}
</style>
