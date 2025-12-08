<template>
  <div class="alert-management">
   
    <el-card class="alert-form" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>创建告警规则</span>
        </div>
      </template>
      <el-form :model="newAlertRule" label-width="100px" :inline="true" @submit.prevent="addAlertRule">
        <el-form-item label="规则名称">
          <el-input v-model="newAlertRule.name" placeholder="输入规则名称" required style="width: 200px" />
        </el-form-item>
        <el-form-item label="指标类型">
          <el-select v-model="newAlertRule.metricType" placeholder="选择指标" style="width: 150px">
            <el-option label="CPU使用率" value="CPU" />
            <el-option label="内存使用率" value="MEMORY" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件">
          <el-select v-model="newAlertRule.condition" style="width: 120px">
            <el-option label="大于" value="GT" />
            <el-option label="小于" value="LT" />
            <el-option label="等于" value="EQ" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="newAlertRule.threshold" :min="0" :max="100" :step="0.01" style="width: 150px" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="newAlertRule.severity" style="width: 120px">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addAlertRule" :icon="Plus">创建规则</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="alert-list" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>告警规则列表</span>
        </div>
      </template>
      <el-table :data="alertRules" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="规则名称" min-width="150" />
        <el-table-column label="指标类型" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.metricType === 'CPU' ? 'danger' : 'primary'" size="small">
              {{ getMetricTypeName(scope.row.metricType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="条件" width="100">
          <template #default="scope">
            {{ getConditionText(scope.row.condition) }}
          </template>
        </el-table-column>
        <el-table-column prop="threshold" label="阈值" width="100">
          <template #default="scope">
            <span class="threshold-value">{{ scope.row.threshold }}</span>
          </template>
        </el-table-column>
        <el-table-column label="严重程度" width="120">
          <template #default="scope">
            <el-tag :type="getSeverityType(scope.row.severity)" size="small">
              {{ getSeverityText(scope.row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="small">
              {{ scope.row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-popconfirm 
              title="确定要删除该告警规则吗？" 
              @confirm="deleteAlertRule(scope.row.id)"
              confirm-button-text="确定"
              cancel-button-text="取消"
            >
              <template #reference>
                <el-button size="small" type="danger" :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无告警规则" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { Bell, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'AlertManagement',
  components: {
    Bell,
    Plus,
    Delete
  },
  data() {
    return {
      alertRules: [],
      loading: false,
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
      this.loading = true
      try {
        const response = await fetch('/api/alerts/rules')
        this.alertRules = await response.json()
      } catch (error) {
        console.error('Error loading alert rules:', error)
        ElMessage.error('加载告警规则失败')
      } finally {
        this.loading = false
      }
    },
    
    async addAlertRule() {
      if (!this.newAlertRule.name) {
        ElMessage.warning('请输入规则名称')
        return
      }
      
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
          ElMessage.success('规则创建成功')
        } else {
          ElMessage.error('规则创建失败')
        }
      } catch (error) {
        console.error('Error adding alert rule:', error)
        ElMessage.error('规则创建失败')
      }
    },
    
    async deleteAlertRule(id) {
      try {
        const response = await fetch(`/api/alerts/rules/${id}`, {
          method: 'DELETE'
        })
        
        if (response.ok) {
          this.alertRules = this.alertRules.filter(rule => rule.id !== id)
          ElMessage.success('规则删除成功')
        } else {
          ElMessage.error('规则删除失败')
        }
      } catch (error) {
        console.error('Error deleting alert rule:', error)
        ElMessage.error('规则删除失败')
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
    },
    
    getSeverityType(severity) {
      const typeMap = {
        'LOW': 'success',
        'MEDIUM': 'warning',
        'HIGH': 'warning',
        'CRITICAL': 'danger'
      }
      return typeMap[severity] || 'info'
    }
  }
}
</script>

<style scoped>
.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #1976d2;
}

.alert-management {
  padding: 20px;
}

.el-page-header {
  margin-bottom: 24px;
}

.alert-form,
.alert-list {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}

.threshold-value {
  font-weight: 600;
  color: #1976d2;
}
</style>