<template>
  <div id="app">
    <header>
      <div class="header-content">
        <h1><span class="icon">📊</span> 监控中心</h1>
        <nav>
          <button @click="activeTab = 'agents'" :class="{ active: activeTab === 'agents' }">
            <span class="icon">🖥️</span> 代理管理
          </button>
          <button @click="activeTab = 'metrics'" :class="{ active: activeTab === 'metrics' }">
            <span class="icon">📈</span> 监控指标
          </button>
          <button @click="activeTab = 'alerts'" :class="{ active: activeTab === 'alerts' }">
            <span class="icon">🔔</span> 告警管理
          </button>
        </nav>
      </div>
    </header>
    
    <main>
      <AgentManagement v-if="activeTab === 'agents'" />
      <MetricCollection v-if="activeTab === 'metrics'" />
      <AlertManagement v-if="activeTab === 'alerts'" />
    </main>
  </div>
</template>

<script>
import AgentManagement from './components/AgentManagement.vue'
import MetricCollection from './components/MetricCollection.vue'
import AlertManagement from './components/AlertManagement.vue'

export default {
  name: 'App',
  components: {
    AgentManagement,
    MetricCollection,
    AlertManagement
  },
  data() {
    return {
      activeTab: 'agents'
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
  background: linear-gradient(135deg, #c5e1f5, #8fc7ed, #5ba3d0);
  min-height: 100vh;
}

#app {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #000000;
  min-height: 100vh;
}

header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 3px solid #1976d2;
  box-shadow: 0 4px 20px rgba(25, 118, 210, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 40px;
}

header h1 {
  color: #1976d2;
  font-size: 28px;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

header h1 .icon {
  font-size: 32px;
}

nav {
  display: flex;
  gap: 15px;
}

nav button {
  padding: 12px 24px;
  background: #ffffff;
  border: 2px solid #e3f2fd;
  color: #1976d2;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 15px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

nav button:hover {
  background: #e3f2fd;
  border-color: #1976d2;
  color: #0d47a1;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(25, 118, 210, 0.25);
}

nav button.active {
  background: #1976d2;
  border-color: #1976d2;
  color: #ffffff;
  box-shadow: 0 4px 15px rgba(25, 118, 210, 0.4);
}

nav button .icon {
  font-size: 18px;
}

main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px 40px;
  min-height: calc(100vh - 140px);
}
</style>