<template>
  <div class="metric-collection">
    <div class="section-header">
      <h2><span class="icon">📈</span> 监控指标</h2>
    </div>
    
    <div class="metric-view card">
      <h3>最新监控数据</h3>
      <div v-if="metrics.length === 0" class="no-data-message">
        <p>📊 暂无监控数据</p>
      </div>
      <div v-else class="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>代理ID</th>
              <th>指标类型</th>
              <th>数值</th>
              <th>时间戳</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="metric in metrics" :key="metric.id">
              <td>{{ metric.id }}</td>
              <td>{{ metric.agentId }}</td>
              <td><span :class="'metric-type metric-' + metric.metricType.toLowerCase()">{{ getMetricTypeName(metric.metricType) }}</span></td>
              <td><span class="metric-value">{{ formatValue(metric.value, metric.metricType) }}</span></td>
              <td>{{ formatTime(metric.timestamp) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MetricCollection',
  data() {
    return {
      metrics: []
    }
  },
  mounted() {
    this.loadMetrics()
    // Auto refresh every 10 seconds
    this.interval = setInterval(() => {
      this.loadMetrics()
    }, 10000)
  },
  beforeUnmount() {
    if (this.interval) {
      clearInterval(this.interval)
    }
  },
  methods: {
    async loadMetrics() {
      try {
        const response = await fetch('/api/metrics')
        this.metrics = await response.json()
      } catch (error) {
        console.error('Error loading metrics:', error)
      }
    },
    
    getMetricTypeName(type) {
      const typeMap = {
        'CPU': 'CPU使用率',
        'MEMORY': '内存使用率'
      }
      return typeMap[type] || type
    },
    
    formatValue(value, type) {
      if (type === 'CPU' || type === 'MEMORY') {
        return value.toFixed(2) + '%'
      }
      return value.toFixed(2)
    },
    
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.metric-view table {
  width: 100%;
  border-collapse: collapse;
}

.metric-view th, .metric-view td {
  border: 1px solid #ccc;
  padding: 8px;
  text-align: left;
}

.metric-view th {
  background-color: #f2f2f2;
}

.no-data-message {
  text-align: center;
  padding: 60px 20px;
  color: #808080;
  font-size: 16px;
}

.no-data-message p {
  margin: 0;
}

.metric-type {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.metric-cpu {
  background: #ffebee;
  color: #c62828;
  border: 1px solid #ef5350;
}

.metric-memory {
  background: #e3f2fd;
  color: #1976d2;
  border: 1px solid #64b5f6;
}

.metric-value {
  font-weight: 600;
  color: #1976d2;
  font-size: 15px;
}

.section-header h2 {
  color: #1976d2;
  font-size: 24px;
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.section-header .icon {
  font-size: 28px;
}

.card {
  background: rgba(255, 255, 255, 0.95);
  border: 2px solid #e3f2fd;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 25px;
  box-shadow: 0 4px 20px rgba(25, 118, 210, 0.1);
}

.card h3 {
  color: #1976d2;
  font-size: 18px;
  margin-bottom: 20px;
  border-bottom: 2px solid #e3f2fd;
  padding-bottom: 10px;
  font-weight: 600;
}

.table-container {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

thead {
  background: #e3f2fd;
}

th {
  padding: 15px;
  text-align: left;
  color: #1976d2;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 2px solid #bbdefb;
}

td {
  padding: 15px;
  color: #000000;
  border-bottom: 1px solid #e3f2fd;
}

tbody tr {
  transition: all 0.3s ease;
}

tbody tr:hover {
  background: #f5f5f5;
}
</style>