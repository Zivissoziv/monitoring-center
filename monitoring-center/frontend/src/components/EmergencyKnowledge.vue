<template>
  <div class="emergency-knowledge">
    
    <!-- Search and Filter Bar -->
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="知识库标题" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="关联规则">
          <el-select v-model="searchForm.alertRuleId" placeholder="全部" clearable style="width: 250px">
            <el-option 
              v-for="rule in alertRules" 
              :key="rule.id" 
              :label="rule.name" 
              :value="rule.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="RefreshIcon">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- Knowledge List -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>知识库列表</span>
          <el-button type="primary" @click="openCreateDialog" :icon="Plus">新建知识库</el-button>
        </div>
      </template>
      
      <el-table :data="paginatedKnowledgeList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="关联告警规则" min-width="250">
          <template #default="scope">
            <el-tag v-for="ruleId in scope.row.alertRuleIds" :key="ruleId" size="small" style="margin-right: 5px;">
              {{ getAlertRuleName(ruleId) }}
            </el-tag>
            <el-tag v-if="!scope.row.alertRuleIds || scope.row.alertRuleIds.length === 0" type="info" size="small">
              未关联
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="步骤数量" width="120">
          <template #default="scope">
            <el-tag type="info" size="small">
              {{ scope.row.steps ? scope.row.steps.length : 0 }} 步
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="scope">
            {{ formatTime(scope.row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openEditDialog(scope.row)" :icon="Edit">编辑</el-button>
            <el-button size="small" type="success" @click="viewKnowledge(scope.row)" :icon="View">查看</el-button>
            <el-popconfirm 
              title="确定要删除该知识库吗？" 
              @confirm="deleteKnowledge(scope.row.id)"
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
          <el-empty description="暂无知识库数据" />
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
    
    <!-- Create/Edit Dialog -->
    <el-dialog 
      v-model="showDialog" 
      :title="isEditMode ? '编辑知识库' : '新建知识库'" 
      width="900px"
      :close-on-click-modal="false"
    >
      <!-- Summary Card for Edit Mode -->
      <el-alert v-if="isEditMode && currentKnowledge.steps.length > 0" 
        type="info" 
        :closable="false" 
        style="margin-bottom: 20px;"
      >
        <template #title>
          <div style="display: flex; align-items: center; gap: 15px;">
            <span>当前已有 <strong>{{ currentKnowledge.steps.length }}</strong> 个步骤</span>
            <el-divider direction="vertical" />
            <span>
              <el-icon><Reading /></el-icon>
              命令步骤: <strong>{{ currentKnowledge.steps.filter(s => s.stepType !== 'URL_JUMP').length }}</strong>
            </span>
            <el-divider direction="vertical" />
            <span>
              <el-icon><Link /></el-icon>
              URL跳转: <strong>{{ currentKnowledge.steps.filter(s => s.stepType === 'URL_JUMP').length }}</strong>
            </span>
          </div>
        </template>
      </el-alert>
      
      <el-form :model="currentKnowledge" label-width="120px">
        <el-form-item label="关联告警规则" required>
          <el-select 
            v-model="currentKnowledge.alertRuleIds" 
            placeholder="请选择告警规则（可多选）" 
            style="width: 100%"
            multiple
            filterable
          >
            <el-option 
              v-for="rule in alertRules" 
              :key="rule.id" 
              :label="rule.name" 
              :value="rule.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="标题" required>
          <el-input v-model="currentKnowledge.title" placeholder="输入应急处理标题" />
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input v-model="currentKnowledge.description" type="textarea" :rows="3" placeholder="输入整体描述" />
        </el-form-item>
        
        <el-divider content-position="left">
          <el-icon><List /></el-icon>
          <span>应急操作步骤</span>
        </el-divider>
        
        <div class="steps-section">
          <div class="steps-header">
            <el-button type="success" @click="addStep" :icon="Plus" size="default">
              <span style="font-weight: 600;">添加步骤</span>
            </el-button>
          </div>
          
          <el-empty v-if="currentKnowledge.steps.length === 0" description="暂无操作步骤，请添加">
            <el-button type="primary" @click="addStep" :icon="Plus">立即添加第一个步骤</el-button>
          </el-empty>
          
          <el-card v-for="(step, index) in currentKnowledge.steps" :key="index" class="step-item" shadow="hover" 
            :style="{ borderLeft: step.stepType === 'URL_JUMP' ? '4px solid #67c23a' : '4px solid #409eff' }">
            <template #header>
              <div class="step-header">
                <div style="display: flex; align-items: center; gap: 10px;">
                  <span class="step-number">步骤 {{ index + 1 }}</span>
                  <el-tag v-if="step.stepType === 'URL_JUMP'" type="success" size="small">
                    <el-icon><Link /></el-icon> URL跳转
                  </el-tag>
                  <el-tag v-else type="primary" size="small">
                    <el-icon><Reading /></el-icon> 执行命令
                  </el-tag>
                </div>
                <div class="step-actions">
                  <el-button-group size="small">
                    <el-button v-if="index > 0" @click="moveStepUp(index)" :icon="Top">上移</el-button>
                    <el-button v-if="index < currentKnowledge.steps.length - 1" @click="moveStepDown(index)" :icon="Bottom">下移</el-button>
                    <el-button type="danger" @click="removeStep(index)" :icon="Delete">删除</el-button>
                  </el-button-group>
                </div>
              </div>
            </template>
            
            <el-form-item label="步骤描述" required>
              <el-input v-model="step.description" placeholder="请描述该步骤的目的" />
            </el-form-item>
            
            <el-form-item label="步骤类型" required>
              <el-radio-group v-model="step.stepType">
                <el-radio label="COMMAND">执行命令</el-radio>
                <el-radio label="URL_JUMP">URL跳转</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item v-if="step.stepType === 'COMMAND'" label="Linux命令" required>
              <el-input v-model="step.linuxCommand" type="textarea" :rows="2" placeholder="输入要执行的Linux命令" class="command-input" />
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                <el-icon><InfoFilled /></el-icon> 命令将在指定的代理机器上执行
              </div>
            </el-form-item>
            
            <el-form-item v-if="step.stepType === 'URL_JUMP'" label="跳转URL" required>
              <el-input v-model="step.jumpUrl" placeholder="输入要跳转的URL地址">
                <template #prepend>
                  <el-icon><Link /></el-icon>
                </template>
              </el-input>
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                <el-icon><InfoFilled /></el-icon> 点击步骤时将在新窗口打开此链接，例如：http://localhost:8088/health
              </div>
            </el-form-item>
            
            <!-- Only show agent selection for COMMAND type -->
            <el-row v-if="step.stepType === 'COMMAND'" :gutter="20">
              <el-col :span="12">
                <el-form-item label="执行代理">
                  <el-select v-model="step.agentId" placeholder="使用告警代理" clearable style="width: 100%">
                    <el-option label="使用告警代理" :value="null" />
                    <el-option 
                      v-for="agent in agents" 
                      :key="agent.id" 
                      :label="`${agent.name} (${agent.ip})`" 
                      :value="agent.id"
                    />
                  </el-select>
                  <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                    不选择则在告警对应的代理上执行
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="依赖步骤">
                  <el-select v-model="step.dependsOnIndex" placeholder="无依赖" style="width: 100%">
                    <el-option label="无依赖" :value="null" />
                    <el-option v-for="i in index" :key="i" :label="`步骤 ${i}`" :value="i - 1" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <!-- For URL_JUMP, only show dependency selection -->
            <el-row v-if="step.stepType === 'URL_JUMP'" :gutter="20">
              <el-col :span="24">
                <el-form-item label="依赖步骤">
                  <el-select v-model="step.dependsOnIndex" placeholder="无依赖" style="width: 100%">
                    <el-option label="无依赖" :value="null" />
                    <el-option v-for="i in index" :key="i" :label="`步骤 ${i}`" :value="i - 1" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="备注">
              <el-input v-model="step.notes" placeholder="额外说明或警告" />
            </el-form-item>
          </el-card>
        </div>
      </el-form>
      
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="saveKnowledge">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- View Dialog -->
    <el-dialog 
      v-model="showViewDialog" 
      title="查看知识库" 
      width="1000px"
    >
      <div v-if="viewingKnowledge">
        <el-descriptions :column="2" border size="large">
          <el-descriptions-item label="标题" :span="2">
            <h3 style="margin: 0; color: #1976d2;">
              <el-icon><Reading /></el-icon>
              {{ viewingKnowledge.title }}
            </h3>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2" v-if="viewingKnowledge.description">
            <div style="padding: 8px; background: #f5f7fa; border-radius: 4px;">
              {{ viewingKnowledge.description }}
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="关联规则" :span="2">
            <el-tag v-for="ruleId in viewingKnowledge.alertRuleIds" :key="ruleId" size="default" type="primary" style="margin-right: 5px;">
              {{ getAlertRuleName(ruleId) }}
            </el-tag>
            <el-tag v-if="!viewingKnowledge.alertRuleIds || viewingKnowledge.alertRuleIds.length === 0" type="info">未关联</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="步骤总数">
            <el-tag type="info" size="large">
              {{ viewingKnowledge.steps ? viewingKnowledge.steps.length : 0 }} 步
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatTime(viewingKnowledge.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
        
        <el-divider content-position="left">
          <el-icon><List /></el-icon>
          <span style="font-weight: 600; font-size: 16px;">应急操作步骤</span>
        </el-divider>
        
        <!-- Steps Table -->
        <el-table 
          :data="viewingKnowledge.steps" 
          stripe 
          border
          style="width: 100%"
          :default-sort="{ prop: 'stepOrder', order: 'ascending' }"
        >
          <el-table-column type="index" label="步骤" width="80" :index="(index) => index + 1" />
          
          <el-table-column label="类型" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.stepType === 'URL_JUMP'" type="success" size="default">
                <el-icon><Link /></el-icon> URL跳转
              </el-tag>
              <el-tag v-else type="primary" size="default">
                <el-icon><Reading /></el-icon> 执行命令
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="描述" min-width="200">
            <template #default="scope">
              <div style="font-weight: 600; color: #303133;">{{ scope.row.description }}</div>
              <div v-if="scope.row.notes" style="font-size: 12px; color: #909399; margin-top: 4px;">
                <el-icon><InfoFilled /></el-icon> {{ scope.row.notes }}
              </div>
            </template>
          </el-table-column>
          
          <el-table-column label="内容" min-width="300">
            <template #default="scope">
              <div v-if="scope.row.stepType === 'URL_JUMP'">
                <el-link :href="scope.row.jumpUrl" target="_blank" :icon="Link" type="success">
                  {{ scope.row.jumpUrl }}
                </el-link>
              </div>
              <pre v-else class="command-text-view">{{ scope.row.linuxCommand }}</pre>
            </template>
          </el-table-column>
          
          <el-table-column label="执行代理" width="150">
            <template #default="scope">
              <div v-if="scope.row.stepType === 'COMMAND'">
                <el-tag v-if="scope.row.agentId" size="default" type="success">
                  {{ getAgentName(scope.row.agentId) }}
                </el-tag>
                <el-tag v-else type="info" size="default">告警代理</el-tag>
              </div>
              <span v-else style="color: #909399; font-size: 12px;">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <template #footer>
        <el-button @click="closeViewDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Reading, Plus, Edit, View, Delete, List, Top, Bottom, Search, Refresh, Link } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'EmergencyKnowledge',
  components: {
    Reading,
    Plus,
    Edit,
    View,
    Delete,
    List,
    Top,
    Bottom,
    Search,
    RefreshIcon: Refresh,
    Link
  },
  data() {
    return {
      knowledgeList: [],
      alertRules: [],
      agents: [],
      loading: false,
      searchForm: {
        title: '',
        alertRuleId: ''
      },
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      showDialog: false,
      showViewDialog: false,
      isEditMode: false,
      currentKnowledge: {
        id: null,
        alertRuleIds: [],
        title: '',
        description: '',
        steps: []
      },
      viewingKnowledge: null
    }
  },
  computed: {
    filteredKnowledgeList() {
      let filtered = this.knowledgeList
      
      if (this.searchForm.title) {
        filtered = filtered.filter(item => 
          item.title.toLowerCase().includes(this.searchForm.title.toLowerCase())
        )
      }
      
      if (this.searchForm.alertRuleId) {
        filtered = filtered.filter(item => 
          item.alertRuleIds && item.alertRuleIds.includes(this.searchForm.alertRuleId)
        )
      }
      
      return filtered
    },
    filteredTotal() {
      return this.filteredKnowledgeList.length
    },
    paginatedKnowledgeList() {
      const start = (this.pagination.currentPage - 1) * this.pagination.pageSize
      const end = start + this.pagination.pageSize
      return this.filteredKnowledgeList.slice(start, end)
    },
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        // Load all knowledge
        const knowledgeResponse = await fetch('/api/emergency')
        this.knowledgeList = await knowledgeResponse.json()
        
        // Load alert rules
        const rulesResponse = await fetch('/api/alert-rules')
        this.alertRules = await rulesResponse.json()
        
        // Load agents
        const agentsResponse = await fetch('/api/agents')
        this.agents = await agentsResponse.json()
      } catch (error) {
        console.error('Error loading data:', error)
        ElMessage.error('加载数据失败')
      } finally {
        this.loading = false
      }
    },
    
    handleSearch() {
      this.pagination.currentPage = 1
    },
    
    handleReset() {
      this.searchForm = {
        title: '',
        alertRuleId: ''
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
    
    getAlertRuleName(alertRuleId) {
      const rule = this.alertRules.find(r => r.id === alertRuleId)
      return rule ? rule.name : '未知规则'
    },
    
    getAgentName(agentId) {
      if (!agentId) return '告警代理'
      const agent = this.agents.find(a => a.id === agentId)
      return agent ? `${agent.name} (${agent.ip})` : '未知代理'
    },
    
    formatTime(timestamp) {
      if (!timestamp) return '-'
      // Handle both ISO string and milliseconds
      const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp)
      return date.toLocaleString('zh-CN')
    },
    
    openCreateDialog() {
      this.isEditMode = false
      this.resetCurrentKnowledge()
      this.showDialog = true
    },
    
    openEditDialog(knowledge) {
      this.isEditMode = true
      this.currentKnowledge = {
        id: knowledge.id,
        alertRuleIds: knowledge.alertRuleIds ? [...knowledge.alertRuleIds] : [],
        title: knowledge.title,
        description: knowledge.description,
        steps: knowledge.steps.map((step, index) => ({
          stepType: step.stepType || 'COMMAND',
          description: step.description,
          linuxCommand: step.linuxCommand,
          jumpUrl: step.jumpUrl,
          agentId: step.agentId || null,
          dependsOnIndex: step.dependsOn ? this.findStepIndex(knowledge.steps, step.dependsOn) : null,
          notes: step.notes || ''
        }))
      }
      this.showDialog = true
    },
    
    closeDialog() {
      this.showDialog = false
      this.resetCurrentKnowledge()
    },
    
    resetCurrentKnowledge() {
      this.currentKnowledge = {
        id: null,
        alertRuleIds: [],
        title: '',
        description: '',
        steps: []
      }
    },
    
    addStep() {
      this.currentKnowledge.steps.push({
        stepType: 'COMMAND',
        description: '',
        linuxCommand: '',
        jumpUrl: '',
        agentId: null,
        dependsOnIndex: null,
        notes: ''
      })
    },
    
    removeStep(index) {
      const hasDependents = this.currentKnowledge.steps.some(
        (step, i) => i > index && step.dependsOnIndex === index
      )
      
      if (hasDependents) {
        ElMessageBox.confirm('有其他步骤依赖于该步骤，确定要删除吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }).then(() => {
          this.currentKnowledge.steps.splice(index, 1)
          // Update dependency indices
          this.currentKnowledge.steps.forEach(step => {
            if (step.dependsOnIndex !== null && step.dependsOnIndex > index) {
              step.dependsOnIndex--
            } else if (step.dependsOnIndex === index) {
              step.dependsOnIndex = null
            }
          })
        }).catch(() => {})
      } else {
        this.currentKnowledge.steps.splice(index, 1)
        // Update dependency indices
        this.currentKnowledge.steps.forEach(step => {
          if (step.dependsOnIndex !== null && step.dependsOnIndex > index) {
            step.dependsOnIndex--
          }
        })
      }
    },
    
    moveStepUp(index) {
      if (index === 0) return
      const temp = this.currentKnowledge.steps[index]
      this.currentKnowledge.steps[index] = this.currentKnowledge.steps[index - 1]
      this.currentKnowledge.steps[index - 1] = temp
      
      // Update dependencies
      this.updateDependenciesAfterMove(index, index - 1)
    },
    
    moveStepDown(index) {
      if (index === this.currentKnowledge.steps.length - 1) return
      const temp = this.currentKnowledge.steps[index]
      this.currentKnowledge.steps[index] = this.currentKnowledge.steps[index + 1]
      this.currentKnowledge.steps[index + 1] = temp
      
      // Update dependencies
      this.updateDependenciesAfterMove(index, index + 1)
    },
    
    updateDependenciesAfterMove(oldIndex, newIndex) {
      this.currentKnowledge.steps.forEach((step, i) => {
        if (step.dependsOnIndex === oldIndex) {
          step.dependsOnIndex = newIndex
        } else if (step.dependsOnIndex === newIndex) {
          step.dependsOnIndex = oldIndex
        }
      })
    },
    
    async saveKnowledge() {
      if (!this.currentKnowledge.alertRuleIds || this.currentKnowledge.alertRuleIds.length === 0) {
        ElMessage.warning('请选择至少一个关联的告警规则')
        return
      }
      
      if (!this.currentKnowledge.title) {
        ElMessage.warning('请输入标题')
        return
      }
      
      // Validate steps
      for (let i = 0; i < this.currentKnowledge.steps.length; i++) {
        const step = this.currentKnowledge.steps[i]
        if (!step.description) {
          alert(`步骤 ${i + 1} 的描述不能为空`)
          return
        }
        if (step.stepType === 'COMMAND' && !step.linuxCommand) {
          alert(`步骤 ${i + 1} 的命令不能为空`)
          return
        }
        if (step.stepType === 'URL_JUMP' && !step.jumpUrl) {
          alert(`步骤 ${i + 1} 的跳转URL不能为空`)
          return
        }
      }
      
      // Prepare data for backend
      const payload = {
        id: this.currentKnowledge.id,
        alertRuleIds: this.currentKnowledge.alertRuleIds,
        title: this.currentKnowledge.title,
        description: this.currentKnowledge.description,
        steps: this.currentKnowledge.steps.map((step, index) => ({
          stepOrder: index + 1,
          stepType: step.stepType || 'COMMAND',
          description: step.description,
          linuxCommand: step.stepType === 'COMMAND' ? step.linuxCommand : null,
          jumpUrl: step.stepType === 'URL_JUMP' ? step.jumpUrl : null,
          agentId: step.agentId || null,
          dependsOn: null,
          notes: step.notes
        }))
      }
      
      try {
        const response = await fetch('/api/emergency', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(payload)
        })
        
        if (response.ok) {
          ElMessage.success('保存成功')
          this.closeDialog()
          this.loadData()
        } else {
          ElMessage.error('保存失败')
        }
      } catch (error) {
        console.error('Error saving knowledge:', error)
        ElMessage.error('保存失败')
      }
    },
    
    async deleteKnowledge(id) {
      try {
        const response = await fetch(`/api/emergency/${id}`, {
          method: 'DELETE'
        })
        
        if (response.ok) {
          ElMessage.success('删除成功')
          this.loadData()
        } else {
          ElMessage.error('删除失败')
        }
      } catch (error) {
        console.error('Error deleting knowledge:', error)
        ElMessage.error('删除失败')
      }
    },
    
    viewKnowledge(knowledge) {
      this.viewingKnowledge = knowledge
      this.showViewDialog = true
    },
    
    closeViewDialog() {
      this.showViewDialog = false
      this.viewingKnowledge = null
    },
    
    findStepIndex(steps, dependsOnId) {
      return steps.findIndex(s => s.id === dependsOnId)
    },
    
    findStepNumber(steps, stepId) {
      const index = steps.findIndex(s => s.id === stepId)
      return index >= 0 ? index + 1 : 0
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

.emergency-knowledge {
  padding: 20px;
}

.el-page-header {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}

.steps-section {
  margin-top: 20px;
}

.steps-header {
  margin-bottom: 15px;
  display: flex;
  justify-content: flex-end;
}

.step-item {
  margin-bottom: 15px;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-number {
  color: #1976d2;
  font-weight: 600;
  font-size: 16px;
}

.command-input {
  font-family: 'Courier New', monospace;
}

.command-view {
  background: #f5f5f5;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 10px;
}

.command-label {
  color: #1976d2;
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 8px;
}

.command-text {
  background: #263238;
  color: #4caf50;
  padding: 10px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.5;
}

.command-text-view {
  background: #f5f5f5;
  color: #303133;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  margin: 0;
  border: 1px solid #dcdfe6;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.4;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
