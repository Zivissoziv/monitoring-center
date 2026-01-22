<template>
  <div class="alert-management">
   
    <!-- Search and Filter Bar -->
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="规则名称">
          <el-input v-model="searchForm.name" placeholder="规则名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="指标类型">
          <el-select v-model="searchForm.metricType" placeholder="全部" clearable style="width: 200px">
            <el-option 
              v-for="def in metricDefinitions" 
              :key="def.metricName" 
              :label="def.displayName" 
              :value="def.metricName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="searchForm.severity" placeholder="全部" clearable style="width: 120px">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.enabled" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="RefreshIcon">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="alert-list" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>告警规则列表</span>
          <el-button type="primary" @click="openCreateDialog" :icon="Plus">新建告警规则</el-button>
        </div>
      </template>
      <el-table :data="paginatedAlertRules" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="规则名称" min-width="150" />
        <el-table-column prop="alertMessage" label="告警信息" min-width="200">
          <template #default="scope">
            <span v-if="scope.row.alertMessage" style="color: #606266;">{{ scope.row.alertMessage }}</span>
            <span v-else style="color: #909399; font-style: italic;">无自定义信息</span>
          </template>
        </el-table-column>
        <el-table-column label="指标类型" width="180">
          <template #default="scope">
            <div style="display: flex; flex-direction: column; gap: 2px;">
              <el-tag :type="getMetricTypeTagColor(scope.row.metricType)" size="small">
                {{ getMetricTypeName(scope.row.metricType) }}
              </el-tag>
              <span style="font-size: 11px; color: #909399;">
                {{ getMetricTypeCategory(scope.row.metricType) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="条件" width="100">
          <template #default="scope">
            {{ getConditionText(scope.row.condition) }}
          </template>
        </el-table-column>
        <el-table-column prop="threshold" label="阈值" width="150">
          <template #default="scope">
            <span class="threshold-value" v-if="scope.row.threshold !== null && scope.row.threshold !== undefined">
              {{ scope.row.threshold }}
            </span>
            <span class="threshold-value" v-else-if="scope.row.thresholdText">
              {{ scope.row.thresholdText }}
            </span>
            <span v-else style="color: #909399;">-</span>
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
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openEditDialog(scope.row)" :icon="Edit">编辑</el-button>
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
      
      <!-- Pagination -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredTotal"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- Create/Edit Alert Rule Dialog -->
    <el-dialog 
      v-model="showDialog" 
      :title="isEditMode ? '编辑告警规则' : '新建告警规则'" 
      width="800px"
      :close-on-click-modal="false"
    >
      <!-- 说明信息 -->
      <el-alert type="info" :closable="false" style="margin-bottom: 15px;">
        <template #title>
          <strong>告警策略说明</strong>
        </template>
        <div style="font-size: 13px; line-height: 1.6;">
          <div><strong>数值型</strong>：支持比较操作（大于、小于、等于等），阈值为数字。示例：CPU使用率 > 80</div>
          <div><strong>布尔型</strong>：支持判断真/假，阈值为 true 或 false。示例：端口状态 = false（端口未监听）</div>
          <div><strong>字符型</strong>：支持字符串匹配（等于、包含等）。示例：应用状态包含 "ERROR"</div>
        </div>
      </el-alert>

      <el-form :model="newAlertRule" label-width="100px">
        <el-form-item label="规则名称" required>
          <el-input v-model="newAlertRule.name" placeholder="输入规则名称" />
        </el-form-item>
        
        <el-form-item label="指标类型" required>
          <el-select v-model="newAlertRule.metricType" placeholder="选择指标" @change="onMetricTypeChange" style="width: 100%;">
            <el-option 
              v-for="def in metricDefinitions" 
              :key="def.metricName" 
              :label="`${def.displayName} (${def.metricName})`" 
              :value="def.metricName"
            >
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>{{ def.displayName }}</span>
                <el-tag :type="getMetricTypeTagColorByType(def.metricType)" size="small">
                  {{ getMetricTypeText(def.metricType) }}
                </el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="条件" required>
              <el-select v-model="newAlertRule.condition" style="width: 100%;">
                <el-option v-for="cond in availableConditions" :key="cond.value" 
                           :label="cond.label" :value="cond.value" />
              </el-select>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <!-- Numeric threshold -->
            <el-form-item label="阈值" required v-if="currentMetricType === 'NUMERIC'">
              <el-input-number v-model="newAlertRule.threshold" :min="0" :step="0.01" style="width: 100%;" />
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                输入数值，如 80, 90, 95.5
              </div>
            </el-form-item>
            
            <!-- Boolean threshold -->
            <el-form-item label="阈值" required v-if="currentMetricType === 'BOOLEAN'">
              <el-select v-model="newAlertRule.thresholdText" style="width: 100%;">
                <el-option label="真 (True)" value="true" />
                <el-option label="假 (False)" value="false" />
              </el-select>
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                选择 true 或 false，用于状态判断
              </div>
            </el-form-item>
            
            <!-- String threshold -->
            <el-form-item label="阈值" required v-if="currentMetricType === 'STRING'">
              <el-input v-model="newAlertRule.thresholdText" placeholder="输入字符串" />
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                输入字符串，如 'ERROR', 'FAILED', 'DOWN'
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="严重程度" required>
          <el-select v-model="newAlertRule.severity" style="width: 200px;">
            <el-option label="低" value="LOW">
              <el-tag type="success" size="small">低</el-tag>
            </el-option>
            <el-option label="中" value="MEDIUM">
              <el-tag type="warning" size="small">中</el-tag>
            </el-option>
            <el-option label="高" value="HIGH">
              <el-tag type="warning" size="small">高</el-tag>
            </el-option>
            <el-option label="严重" value="CRITICAL">
              <el-tag type="danger" size="small">严重</el-tag>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="告警信息">
          <el-input 
            v-model="newAlertRule.alertMessage" 
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="输入自定义告警信息，如：警告！CPU使用率过高，请检查系统负载"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            自定义告警时展示的提示信息，帮助快速了解告警内容
          </div>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-switch v-model="newAlertRule.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="isEditMode ? updateAlertRule() : addAlertRule()">
          {{ isEditMode ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Bell, Plus, Delete, QuestionFilled, Refresh, Edit, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'AlertManagement',
  components: {
    Bell,
    Plus,
    Delete,
    QuestionFilled,
    Refresh,
    Edit,
    Search,
    RefreshIcon: Refresh
  },
  data() {
    return {
      alertRules: [],
      metricDefinitions: [],
      loading: false,
      searchForm: {
        name: '',
        metricType: '',
        severity: '',
        enabled: ''
      },
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      showDialog: false,
      isEditMode: false,
      editingRuleId: null,
      currentMetricType: 'NUMERIC',
      newAlertRule: {
        name: '',
        metricType: '',
        condition: 'GT',
        threshold: 80.0,
        thresholdText: '',
        severity: 'HIGH',
        alertMessage: '',
        enabled: true
      }
    }
  },
  mounted() {
    this.loadAlertRules()
    this.loadMetricDefinitions()
  },
  computed: {
    filteredAlertRules() {
      let filtered = this.alertRules
      
      if (this.searchForm.name) {
        filtered = filtered.filter(rule => 
          rule.name.toLowerCase().includes(this.searchForm.name.toLowerCase())
        )
      }
      
      if (this.searchForm.metricType) {
        filtered = filtered.filter(rule => 
          rule.metricType === this.searchForm.metricType
        )
      }
      
      if (this.searchForm.severity) {
        filtered = filtered.filter(rule => 
          rule.severity === this.searchForm.severity
        )
      }
      
      if (this.searchForm.enabled !== '') {
        filtered = filtered.filter(rule => 
          rule.enabled === this.searchForm.enabled
        )
      }
      
      return filtered
    },
    filteredTotal() {
      return this.filteredAlertRules.length
    },
    paginatedAlertRules() {
      const start = (this.pagination.currentPage - 1) * this.pagination.pageSize
      const end = start + this.pagination.pageSize
      return this.filteredAlertRules.slice(start, end)
    },
    availableConditions() {
      switch (this.currentMetricType) {
        case 'NUMERIC':
          return [
            { label: '大于', value: 'GT' },
            { label: '大于等于', value: 'GTE' },
            { label: '小于', value: 'LT' },
            { label: '小于等于', value: 'LTE' },
            { label: '等于', value: 'EQUALS' },
            { label: '不等于', value: 'NOT_EQUALS' }
          ]
        case 'BOOLEAN':
          return [
            { label: '等于', value: 'EQUALS' },
            { label: '不等于', value: 'NOT_EQUALS' }
          ]
        case 'STRING':
          return [
            { label: '等于', value: 'EQUALS' },
            { label: '不等于', value: 'NOT_EQUALS' },
            { label: '包含', value: 'CONTAINS' },
            { label: '不包含', value: 'NOT_CONTAINS' }
          ]
        default:
          return [
            { label: '大于', value: 'GT' },
            { label: '小于', value: 'LT' },
            { label: '等于', value: 'EQUALS' }
          ]
      }
    }
  },
  methods: {
    async loadAlertRules() {
      this.loading = true
      try {
        const response = await fetch('/api/alert-rules')
        this.alertRules = await response.json()
      } catch (error) {
        console.error('Error loading alert rules:', error)
        ElMessage.error('加载告警规则失败')
      } finally {
        this.loading = false
      }
    },
    
    handleSearch() {
      this.pagination.currentPage = 1
    },
    
    handleReset() {
      this.searchForm = {
        name: '',
        metricType: '',
        severity: '',
        enabled: ''
      }
      this.pagination.currentPage = 1
    },
    
    handleSizeChange(val) {
      this.pagination.pageSize = val
      this.pagination.currentPage = 1
    },
    
    handleCurrentChange(val) {
      this.pagination.currentPage = val
    },
    
    async loadMetricDefinitions() {
      try {
        const response = await fetch('/api/metric-definitions')
        this.metricDefinitions = await response.json()
      } catch (error) {
        console.error('Error loading metric definitions:', error)
      }
    },
    
    openCreateDialog() {
      this.isEditMode = false
      this.editingRuleId = null
      this.resetAlertRule()
      this.showDialog = true
      // Set default metric type if available
      if (this.metricDefinitions.length > 0 && !this.newAlertRule.metricType) {
        this.newAlertRule.metricType = this.metricDefinitions[0].metricName
        this.onMetricTypeChange()
      }
    },
    
    openEditDialog(rule) {
      this.isEditMode = true
      this.editingRuleId = rule.id
      
      // Find metric definition to get metric type
      const metricDef = this.metricDefinitions.find(def => def.metricName === rule.metricType)
      this.currentMetricType = metricDef ? metricDef.metricType : 'NUMERIC'
      
      // Populate form with existing rule data
      this.newAlertRule = {
        name: rule.name,
        metricType: rule.metricType,
        condition: rule.condition,
        threshold: rule.threshold,
        thresholdText: rule.thresholdText,
        severity: rule.severity,
        alertMessage: rule.alertMessage || '',
        enabled: rule.enabled
      }
      
      this.showDialog = true
    },
    
    closeDialog() {
      this.showDialog = false
      this.isEditMode = false
      this.editingRuleId = null
      this.resetAlertRule()
    },
    
    resetAlertRule() {
      this.newAlertRule = {
        name: '',
        metricType: this.metricDefinitions.length > 0 ? this.metricDefinitions[0].metricName : '',
        condition: 'GT',
        threshold: 80.0,
        thresholdText: '',
        severity: 'HIGH',
        alertMessage: '',
        enabled: true
      }
      if (this.newAlertRule.metricType) {
        this.onMetricTypeChange()
      }
    },
    
    onMetricTypeChange() {
      const selectedMetric = this.metricDefinitions.find(
        def => def.metricName === this.newAlertRule.metricType
      )
      if (selectedMetric) {
        this.currentMetricType = selectedMetric.metricType || 'NUMERIC'
        // Reset condition based on metric type
        if (this.currentMetricType === 'NUMERIC') {
          this.newAlertRule.condition = 'GT'
          this.newAlertRule.threshold = 80.0
          this.newAlertRule.thresholdText = ''
        } else if (this.currentMetricType === 'BOOLEAN') {
          this.newAlertRule.condition = 'EQUALS'
          this.newAlertRule.threshold = null
          this.newAlertRule.thresholdText = 'true'
        } else if (this.currentMetricType === 'STRING') {
          this.newAlertRule.condition = 'EQUALS'
          this.newAlertRule.threshold = null
          this.newAlertRule.thresholdText = ''
        }
      }
    },
    
    async addAlertRule() {
      if (!this.newAlertRule.name) {
        ElMessage.warning('请输入规则名称')
        return
      }
      
      if (!this.newAlertRule.metricType) {
        ElMessage.warning('请选择指标类型')
        return
      }
      
      // Validate threshold based on metric type
      if (this.currentMetricType === 'NUMERIC' && (this.newAlertRule.threshold === null || this.newAlertRule.threshold === undefined)) {
        ElMessage.warning('请输入数值阈值')
        return
      }
      if ((this.currentMetricType === 'BOOLEAN' || this.currentMetricType === 'STRING') && !this.newAlertRule.thresholdText) {
        ElMessage.warning('请输入阈值')
        return
      }
      
      try {
        const response = await fetch('/api/alert-rules', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.newAlertRule)
        })
        
        if (response.ok) {
          ElMessage.success('规则创建成功')
          this.closeDialog()
          await this.loadAlertRules()
        } else {
          ElMessage.error('规则创建失败')
        }
      } catch (error) {
        console.error('Error adding alert rule:', error)
        ElMessage.error('规则创建失败')
      }
    },
    
    async updateAlertRule() {
      if (!this.newAlertRule.name) {
        ElMessage.warning('请输入规则名称')
        return
      }
      
      if (!this.newAlertRule.metricType) {
        ElMessage.warning('请选择指标类型')
        return
      }
      
      // Validate threshold based on metric type
      if (this.currentMetricType === 'NUMERIC' && (this.newAlertRule.threshold === null || this.newAlertRule.threshold === undefined)) {
        ElMessage.warning('请输入数值阈值')
        return
      }
      if ((this.currentMetricType === 'BOOLEAN' || this.currentMetricType === 'STRING') && !this.newAlertRule.thresholdText) {
        ElMessage.warning('请输入阈值')
        return
      }
      
      try {
        const response = await fetch(`/api/alert-rules/${this.editingRuleId}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.newAlertRule)
        })
        
        if (response.ok) {
          ElMessage.success('规则修改成功')
          this.closeDialog()
          await this.loadAlertRules()
        } else {
          ElMessage.error('规则修改失败')
        }
      } catch (error) {
        console.error('Error updating alert rule:', error)
        ElMessage.error('规则修改失败')
      }
    },
    
    async deleteAlertRule(id) {
      try {
        const response = await fetch(`/api/alert-rules/${id}`, {
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
      const metric = this.metricDefinitions.find(def => def.metricName === type)
      return metric ? metric.displayName : type
    },
    
    getMetricTypeCategory(metricName) {
      const metric = this.metricDefinitions.find(def => def.metricName === metricName)
      if (!metric || !metric.metricType) return ''
      
      const typeMap = {
        'NUMERIC': '数值型',
        'BOOLEAN': '布尔型',
        'STRING': '字符型'
      }
      return typeMap[metric.metricType] || ''
    },
    
    getMetricTypeTagColor(metricName) {
      const metric = this.metricDefinitions.find(def => def.metricName === metricName)
      if (!metric || !metric.metricType) return 'info'
      
      const colorMap = {
        'NUMERIC': 'primary',
        'BOOLEAN': 'success',
        'STRING': 'warning'
      }
      return colorMap[metric.metricType] || 'info'
    },
    
    getConditionText(condition) {
      const conditionMap = {
        'GT': '大于',
        'GTE': '大于等于',
        'LT': '小于',
        'LTE': '小于等于',
        'EQ': '等于',
        'EQUALS': '等于',
        'NOT_EQUALS': '不等于',
        'CONTAINS': '包含',
        'NOT_CONTAINS': '不包含'
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
    },
    
    getMetricTypeText(type) {
      const typeMap = {
        'NUMERIC': '数值型',
        'BOOLEAN': '布尔型',
        'STRING': '字符型'
      }
      return typeMap[type] || type
    },
    
    getMetricTypeTagColorByType(type) {
      const colorMap = {
        'NUMERIC': 'primary',
        'BOOLEAN': 'success',
        'STRING': 'warning'
      }
      return colorMap[type] || 'info'
    }
  }
}
</script>

<style scoped>
.alert-management {
  padding: 20px;
}

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

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>