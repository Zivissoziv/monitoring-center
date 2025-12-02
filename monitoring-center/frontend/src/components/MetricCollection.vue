<template>
  <div class="metric-collection">
    <div class="section-header">
      <h2><span class="icon">📈</span> 监控指标</h2>
    </div>
    
    <!-- Filter and View Controls -->
    <div class="control-panel card">
      <div class="filter-section">
        <div class="filter-group">
          <label>指标类型:</label>
          <select v-model="selectedMetricType" @change="filterMetrics" class="filter-select">
            <option value="ALL">全部指标</option>
            <option value="CPU">CPU使用率</option>
            <option value="MEMORY">内存使用率</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>代理:</label>
          <select v-model="selectedAgent" @change="filterMetrics" class="filter-select">
            <option value="ALL">全部代理</option>
            <option v-for="agentId in uniqueAgentIds" :key="agentId" :value="agentId">
              代理 {{ agentId }}
            </option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>时间范围:</label>
          <select v-model="timeRange" @change="filterMetrics" class="filter-select">
            <option value="10">最近10条</option>
            <option value="20">最近20条</option>
            <option value="50">最近50条</option>
            <option value="100">最近100条</option>
            <option value="200">最近200条</option>
            <option value="500">最近500条</option>
          </select>
        </div>
      </div>
      
      <div class="view-toggle">
        <button 
          :class="['toggle-btn', { active: viewMode === 'table' }]" 
          @click="viewMode = 'table'"
        >
          📋 表格视图
        </button>
        <button 
          :class="['toggle-btn', { active: viewMode === 'chart' }]" 
          @click="viewMode = 'chart'"
        >
          📊 图表视图
        </button>
      </div>
    </div>
    
    <!-- Table View -->
    <div v-if="viewMode === 'table'" class="metric-view card">
      <h3>监控数据表格</h3>
      <div v-if="filteredMetrics.length === 0" class="no-data-message">
        <p>📊 暂无符合条件的监控数据</p>
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
            <tr v-for="metric in paginatedMetrics" :key="metric.id">
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
    
    <!-- Chart View -->
    <div v-if="viewMode === 'chart'" class="metric-view card">
      <h3>监控数据图表</h3>
      <div v-if="filteredMetrics.length === 0" class="no-data-message">
        <p>📊 暂无符合条件的监控数据</p>
      </div>
      <div v-else class="chart-container-wrapper">
        <div v-if="filteredMetrics.length > 100" class="chart-warning">
          <p>ℹ️ 数据点较多({{ filteredMetrics.length }}个)，图表已自动采样以提高性能，仅显示最新100个数据点</p>
        </div>
        <div class="chart-container">
          <canvas ref="metricsChart"></canvas>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Chart } from 'chart.js/auto'

export default {
  name: 'MetricCollection',
  data() {
    return {
      metrics: [],
      filteredMetrics: [],
      selectedMetricType: 'ALL',
      selectedAgent: 'ALL',
      timeRange: '100', // Increased default
      viewMode: 'table', // 'table' or 'chart'
      chartInstance: null,
      interval: null
    }
  },
  computed: {
    uniqueAgentIds() {
      const agentIds = [...new Set(this.metrics.map(m => m.agentId))]
      return agentIds.sort((a, b) => a - b)
    },
    paginatedMetrics() {
      return this.filteredMetrics.slice(0, parseInt(this.timeRange))
    }
  },
  watch: {
    viewMode(newMode) {
      if (newMode === 'chart') {
        this.$nextTick(() => {
          this.renderChart()
        })
      }
    },
    filteredMetrics() {
      if (this.viewMode === 'chart') {
        this.$nextTick(() => {
          this.renderChart()
        })
      }
    }
  },
  mounted() {
    this.loadAllData()
    // Auto refresh every 10 seconds
    this.interval = setInterval(() => {
      this.loadAllData()
    }, 10000)
  },
  beforeUnmount() {
    if (this.interval) {
      clearInterval(this.interval)
    }
    if (this.chartInstance) {
      this.chartInstance.destroy()
    }
  },
  methods: {
    async loadAllData() {
      try {
        // Load metrics
        const metricsResponse = await fetch('/api/metrics')
        this.metrics = await metricsResponse.json()
        // Sort by timestamp descending (newest first)
        this.metrics.sort((a, b) => b.timestamp - a.timestamp)
        
        this.filterMetrics()
      } catch (error) {
        console.error('Error loading data:', error)
      }
    },
    
    filterMetrics() {
      let filtered = this.metrics
      
      // Filter by metric type
      if (this.selectedMetricType !== 'ALL') {
        filtered = filtered.filter(m => m.metricType === this.selectedMetricType)
      }
      
      // Filter by agent
      if (this.selectedAgent !== 'ALL') {
        filtered = filtered.filter(m => m.agentId === parseInt(this.selectedAgent))
      }
      
      this.filteredMetrics = filtered
    },
    
    renderChart() {
      if (!this.$refs.metricsChart) return
      
      try {
        // Destroy existing chart
        if (this.chartInstance) {
          this.chartInstance.destroy()
        }
        
        const ctx = this.$refs.metricsChart.getContext('2d')
        
        // Prepare data for chart - limit to 100 data points for performance
        const chartData = this.prepareChartData()
        
        this.chartInstance = new Chart(ctx, {
          type: 'line',
          data: chartData,
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              title: {
                display: true,
                text: this.getChartTitle(),
                font: {
                  size: 16,
                  weight: 'bold'
                },
                color: '#1976d2'
              },
              legend: {
                display: true,
                position: 'top',
                labels: {
                  color: '#000000',
                  font: {
                    size: 12,
                    weight: '600'
                  }
                }
              },
              tooltip: {
                mode: 'index',
                intersect: false,
                backgroundColor: 'rgba(0, 0, 0, 0.8)',
                titleColor: '#ffffff',
                bodyColor: '#ffffff',
                borderColor: '#1976d2',
                borderWidth: 1
              }
            },
            scales: {
              y: {
                beginAtZero: true,
                max: 100,
                ticks: {
                  callback: function(value) {
                    return value + '%'
                  },
                  color: '#000000',
                  font: {
                    weight: '600'
                  }
                },
                grid: {
                  color: 'rgba(0, 0, 0, 0.1)'
                }
              },
              x: {
                ticks: {
                  color: '#000000',
                  font: {
                    weight: '600'
                  },
                  maxTicksLimit: 15, // Increased limit
                  maxRotation: 45,
                  minRotation: 45
                },
                grid: {
                  color: 'rgba(0, 0, 0, 0.1)'
                }
              }
            }
          }
        })
      } catch (error) {
        console.error('Error rendering chart:', error)
        // Display error message to user
        if (this.$refs.metricsChart) {
          const ctx = this.$refs.metricsChart.getContext('2d')
          ctx.clearRect(0, 0, this.$refs.metricsChart.width, this.$refs.metricsChart.height)
          ctx.fillStyle = '#000000'
          ctx.font = '16px Arial'
          ctx.fillText('图表渲染失败，请刷新页面重试', 50, 50)
        }
      }
    },
    
    prepareChartData() {
      // Limit data points to prevent chart clutter (max 100 points)
      const maxDataPoints = 100;
      let data = this.paginatedMetrics.slice().reverse() // Reverse to show oldest to newest
      
      // Sample data if there are too many points
      if (data.length > maxDataPoints) {
        const step = Math.ceil(data.length / maxDataPoints);
        data = data.filter((_, index) => index % step === 0);
      }
      
      const labels = data.map(m => this.formatTimeShort(m.timestamp))
      
      // Group by metric type and agent
      const datasets = []
      
      if (this.selectedMetricType === 'ALL') {
        // Show both CPU and Memory
        const cpuData = data.filter(m => m.metricType === 'CPU').map(m => m.value)
        const memoryData = data.filter(m => m.metricType === 'MEMORY').map(m => m.value)
        
        if (cpuData.length > 0) {
          datasets.push({
            label: 'CPU使用率',
            data: cpuData,
            borderColor: '#c62828',
            backgroundColor: 'rgba(198, 40, 40, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            pointRadius: data.length > 50 ? 0 : 2, // Hide points when too many data points
            pointHoverRadius: 4
          })
        }
        
        if (memoryData.length > 0) {
          datasets.push({
            label: '内存使用率',
            data: memoryData,
            borderColor: '#1976d2',
            backgroundColor: 'rgba(25, 118, 210, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            pointRadius: data.length > 50 ? 0 : 2, // Hide points when too many data points
            pointHoverRadius: 4
          })
        }
      } else {
        // Show selected metric type, group by agent if needed
        if (this.selectedAgent === 'ALL') {
          // Show data for each agent separately
          const agentIds = [...new Set(data.map(m => m.agentId))]
          const colors = ['#c62828', '#1976d2', '#388e3c', '#f57c00', '#7b1fa2']
          
          agentIds.forEach((agentId, index) => {
            const agentData = data.filter(m => m.agentId === agentId).map(m => m.value)
            datasets.push({
              label: `代理${agentId} - ${this.getMetricTypeName(this.selectedMetricType)}`,
              data: agentData,
              borderColor: colors[index % colors.length],
              backgroundColor: colors[index % colors.length] + '20',
              borderWidth: 2,
              tension: 0.4,
              pointRadius: data.length > 30 ? 0 : 2, // Hide points when too many data points
              pointHoverRadius: 4
            })
          })
        } else {
          // Show data for selected agent
          const values = data.map(m => m.value)
          const color = this.selectedMetricType === 'CPU' ? '#c62828' : '#1976d2'
          datasets.push({
            label: `代理${this.selectedAgent} - ${this.getMetricTypeName(this.selectedMetricType)}`,
            data: values,
            borderColor: color,
            backgroundColor: color + '20',
            borderWidth: 2,
            tension: 0.4,
            pointRadius: data.length > 50 ? 0 : 2, // Hide points when too many data points
            pointHoverRadius: 4
          })
        }
      }
      
      return {
        labels,
        datasets
      }
    },
    
    getChartTitle() {
      let title = '监控指标趋势'
      if (this.selectedMetricType !== 'ALL') {
        title = this.getMetricTypeName(this.selectedMetricType) + '趋势'
      }
      if (this.selectedAgent !== 'ALL') {
        title += ` - 代理${this.selectedAgent}`
      }
      return title
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
    },
    
    formatTimeShort(timestamp) {
      const date = new Date(timestamp)
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    }
  }
}
</script>

<style scoped>
.control-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 20px;
}

.filter-section {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-group label {
  font-weight: 600;
  color: #1976d2;
  font-size: 14px;
  white-space: nowrap;
}

.filter-select {
  padding: 8px 15px;
  border: 2px solid #1976d2;
  border-radius: 6px;
  background: #ffffff;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
}

.filter-select:hover {
  border-color: #0d47a1;
  background: #e3f2fd;
}

.filter-select:focus {
  outline: none;
  border-color: #0d47a1;
  box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
}

.view-toggle {
  display: flex;
  gap: 10px;
}

.toggle-btn {
  padding: 10px 20px;
  border: 2px solid #1976d2;
  border-radius: 6px;
  background: #ffffff;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.toggle-btn:hover {
  background: #e3f2fd;
  border-color: #0d47a1;
}

.toggle-btn.active {
  background: #1976d2;
  color: #ffffff;
  border-color: #1976d2;
}

.chart-container-wrapper {
  position: relative;
}

.chart-warning {
  background: #fff3e0;
  border: 1px solid #ffa726;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 15px;
  text-align: center;
  color: #ef6c00;
  font-weight: 600;
}

.chart-container {
  position: relative;
  height: 500px; /* Increased height */
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.metric-view table {
  width: 100%;
  border-collapse: collapse;
}

.metric-view th, .metric-view td {
  border: 1px solid #e0e0e0;
  padding: 8px;
  text-align: left;
  color: #1976d2;
}

.metric-view th {
  background-color: #e3f2fd;
  color: #1976d2;
}

.no-data-message {
  text-align: center;
  padding: 60px 20px;
  color: #9e9e9e;
  font-size: 16px;
  font-weight: 600;
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
</style>