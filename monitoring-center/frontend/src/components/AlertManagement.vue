<template>
  <div class="alert-management">
    <div class="section-header">
      <h2><span class="icon">🔔</span> 告警管理</h2>
    </div>
    
    <div class="alert-form card">
      <h3>创建告警规则</h3>
      <form @submit.prevent="addAlertRule">
        <div class="form-row">
          <div class="form-group">
            <label>规则名称：</label>
            <input v-model="newAlertRule.name" required placeholder="输入规则名称" />
          </div>
          <div class="form-group">
            <label>指标类型：</label>
            <select v-model="newAlertRule.metricType" required>
              <option value="CPU">CPU使用率</option>
              <option value="MEMORY">内存使用率</option>
            </select>
          </div>
          <div class="form-group">
            <label>条件：</label>
            <select v-model="newAlertRule.condition" required>
              <option value="GT">大于</option>
              <option value="LT">小于</option>
              <option value="EQ">等于</option>
            </select>
          </div>
          <div class="form-group">
            <label>阈值：</label>
            <input v-model.number="newAlertRule.threshold" type="number" step="0.01" required placeholder="80.0" />
          </div>
          <div class="form-group">
            <label>严重程度：</label>
            <select v-model="newAlertRule.severity" required>
              <option value="LOW">低</option>
              <option value="MEDIUM">中</option>
              <option value="HIGH">高</option>
              <option value="CRITICAL">严重</option>
            </select>
          </div>
          <button type="submit" class="btn-primary">创建规则</button>
        </div>
      </form>
    </div>
    
    <div class="alert-list card">
      <h3>告警规则列表</h3>
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>规则名称</th>
              <th>指标类型</th>
              <th>条件</th>
              <th>阈值</th>
              <th>严重程度</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in alertRules" :key="rule.id">
              <td>{{ rule.id }}</td>
              <td>{{ rule.name }}</td>
              <td><span :class="'metric-type metric-' + rule.metricType.toLowerCase()">{{ getMetricTypeName(rule.metricType) }}</span></td>
              <td>{{ getConditionText(rule.condition) }}</td>
              <td><span class="threshold-value">{{ rule.threshold }}</span></td>
              <td><span :class="'severity severity-' + rule.severity.toLowerCase()">{{ getSeverityText(rule.severity) }}</span></td>
              <td><span :class="rule.enabled ? 'status-enabled' : 'status-disabled'">{{ rule.enabled ? '启用' : '禁用' }}</span></td>
              <td>
                <button @click="deleteAlertRule(rule.id)" class="btn-danger">删除</button>
              </td>
            </tr>
            <tr v-if="alertRules.length === 0">
              <td colspan="8" class="no-data">暂无告警规则</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AlertManagement',
  data() {
    return {
      alertRules: [],
      newAlertRule: {
        name: '',
        metricType: 'CPU',
        condition: 'GT',
        threshold: 80.0,
        severity: 'HIGH',
        enabled: true
      }
    }
  },
  mounted() {
    this.loadAlertRules()
  },
  methods: {
    async loadAlertRules() {
      try {
        const response = await fetch('/api/alerts/rules')
        this.alertRules = await response.json()
      } catch (error) {
        console.error('Error loading alert rules:', error)
      }
    },
    
    async addAlertRule() {
      try {
        const response = await fetch('/api/alerts/rules', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.newAlertRule)
        })
        
        if (response.ok) {
          const rule = await response.json()
          this.alertRules.push(rule)
          this.newAlertRule = {
            name: '',
            metricType: 'CPU',
            condition: 'GT',
            threshold: 80.0,
            severity: 'HIGH',
            enabled: true
          }
        }
      } catch (error) {
        console.error('Error adding alert rule:', error)
      }
    },
    
    async deleteAlertRule(id) {
      if (!confirm('确定要删除该告警规则吗？')) return
      
      try {
        const response = await fetch(`/api/alerts/rules/${id}`, {
          method: 'DELETE'
        })
        
        if (response.ok) {
          this.alertRules = this.alertRules.filter(rule => rule.id !== id)
        }
      } catch (error) {
        console.error('Error deleting alert rule:', error)
      }
    },
    
    getMetricTypeName(type) {
      const typeMap = {
        'CPU': 'CPU使用率',
        'MEMORY': '内存使用率'
      }
      return typeMap[type] || type
    },
    
    getConditionText(condition) {
      const conditionMap = {
        'GT': '大于',
        'LT': '小于',
        'EQ': '等于'
      }
      return conditionMap[condition] || condition
    },
    
    getSeverityText(severity) {
      const severityMap = {
        'LOW': '低',
        'MEDIUM': '中',
        'HIGH': '高',
        'CRITICAL': '严重'
      }
      return severityMap[severity] || severity
    }
  }
}
</script>

<style scoped>
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

.form-row {
  display: flex;
  gap: 15px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
}

.form-group input,
.form-group select {
  padding: 10px 15px;
  background: #ffffff;
  border: 2px solid #e3f2fd;
  border-radius: 6px;
  color: #1976d2;
  font-size: 14px;
  min-width: 180px;
  transition: all 0.3s ease;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 10px rgba(25, 118, 210, 0.2);
}

.form-group input::placeholder {
  color: #90caf9;
}

.btn-primary {
  padding: 10px 24px;
  background: #1976d2;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  background: #1565c0;
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(25, 118, 210, 0.3);
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

.threshold-value {
  font-weight: 600;
  color: #1976d2;
  font-size: 15px;
}

.severity {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.severity-low {
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #66bb6a;
}

.severity-medium {
  background: #fff3e0;
  color: #ef6c00;
  border: 1px solid #ffa726;
}

.severity-high {
  background: #fff3e0;
  color: #f57c00;
  border: 1px solid #ff9800;
}

.severity-critical {
  background: #ffebee;
  color: #c62828;
  border: 1px solid #ef5350;
}

.status-enabled {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #66bb6a;
}

.status-disabled {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  background: #f5f5f5;
  color: #757575;
  border: 1px solid #bdbdbd;
}

.btn-danger {
  padding: 6px 16px;
  background: #d32f2f;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-danger:hover {
  background: #c62828;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(211, 47, 47, 0.3);
}

.no-data {
  text-align: center;
  color: #9e9e9e;
  font-style: italic;
  padding: 30px !important;
}
</style>