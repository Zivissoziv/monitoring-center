<template>
  <div class="emergency-knowledge">
    
    <!-- Knowledge List -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>知识库列表</span>
          <el-button type="primary" @click="openCreateDialog" :icon="Plus">新建知识库</el-button>
        </div>
      </template>
      
      <el-table :data="knowledgeList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="关联告警规则" min-width="180">
          <template #default="scope">
            {{ getAlertRuleName(scope.row.alertRuleId) }}
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
    </el-card>
    
    <!-- Create/Edit Dialog -->
    <el-dialog 
      v-model="showDialog" 
      :title="isEditMode ? '编辑知识库' : '新建知识库'" 
      width="900px"
      :close-on-click-modal="false"
    >
      <el-form :model="currentKnowledge" label-width="120px">
        <el-form-item label="关联告警规则" required>
          <el-select v-model="currentKnowledge.alertRuleId" :disabled="isEditMode" placeholder="请选择告警规则" style="width: 100%">
            <el-option 
              v-for="rule in availableAlertRules" 
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
            <el-button type="success" @click="addStep" :icon="Plus" size="small">添加步骤</el-button>
          </div>
          
          <el-empty v-if="currentKnowledge.steps.length === 0" description="暂无操作步骤，请添加" />
          
          <el-card v-for="(step, index) in currentKnowledge.steps" :key="index" class="step-item" shadow="hover">
            <template #header>
              <div class="step-header">
                <span class="step-number">步骤 {{ index + 1 }}</span>
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
            
            <el-form-item label="Linux命令" required>
              <el-input v-model="step.linuxCommand" type="textarea" :rows="2" placeholder="输入要执行的Linux命令" class="command-input" />
            </el-form-item>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="依赖步骤">
                  <el-select v-model="step.dependsOnIndex" placeholder="无依赖" style="width: 100%">
                    <el-option label="无依赖" :value="null" />
                    <el-option v-for="i in index" :key="i" :label="`步骤 ${i}`" :value="i - 1" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="备注">
                  <el-input v-model="step.notes" placeholder="额外说明或警告" />
                </el-form-item>
              </el-col>
            </el-row>
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
      width="800px"
    >
      <div v-if="viewingKnowledge">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题" :span="2">
            <h3 style="margin: 0; color: #1976d2;">{{ viewingKnowledge.title }}</h3>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2" v-if="viewingKnowledge.description">
            {{ viewingKnowledge.description }}
          </el-descriptions-item>
          <el-descriptions-item label="关联规则">
            {{ getAlertRuleName(viewingKnowledge.alertRuleId) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatTime(viewingKnowledge.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
        
        <el-divider content-position="left">
          <el-icon><List /></el-icon>
          <span>应急操作步骤</span>
        </el-divider>
        
        <el-timeline>
          <el-timeline-item 
            v-for="(step, index) in viewingKnowledge.steps" 
            :key="index"
            :timestamp="`步骤 ${index + 1}`"
            placement="top"
          >
            <el-card shadow="hover">
              <template #header>
                <div style="font-weight: 600; color: #1976d2;">{{ step.description }}</div>
              </template>
              <div class="command-view">
                <div class="command-label">执行命令：</div>
                <pre class="command-text">{{ step.linuxCommand }}</pre>
              </div>
              <el-alert v-if="step.dependsOn" type="warning" :closable="false" style="margin-top: 10px;">
                依赖步骤: 步骤 {{ findStepNumber(viewingKnowledge.steps, step.dependsOn) }}
              </el-alert>
              <el-alert v-if="step.notes" type="info" :closable="false" style="margin-top: 10px;">
                {{ step.notes }}
              </el-alert>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
      
      <template #footer>
        <el-button @click="closeViewDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Reading, Plus, Edit, View, Delete, List, Top, Bottom } from '@element-plus/icons-vue'
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
    Bottom
  },
  data() {
    return {
      knowledgeList: [],
      alertRules: [],
      loading: false,
      showDialog: false,
      showViewDialog: false,
      isEditMode: false,
      currentKnowledge: {
        id: null,
        alertRuleId: null,
        title: '',
        description: '',
        steps: []
      },
      viewingKnowledge: null
    }
  },
  computed: {
    availableAlertRules() {
      // Filter out alert rules that already have knowledge base
      const usedRuleIds = this.knowledgeList
        .filter(k => !this.isEditMode || k.id !== this.currentKnowledge.id)
        .map(k => k.alertRuleId)
      return this.alertRules.filter(rule => !usedRuleIds.includes(rule.id))
    }
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
        const rulesResponse = await fetch('/api/alerts/rules')
        this.alertRules = await rulesResponse.json()
      } catch (error) {
        console.error('Error loading data:', error)
        ElMessage.error('加载数据失败')
      } finally {
        this.loading = false
      }
    },
    
    getAlertRuleName(alertRuleId) {
      const rule = this.alertRules.find(r => r.id === alertRuleId)
      return rule ? rule.name : '未知规则'
    },
    
    formatTime(timestamp) {
      if (!timestamp) return '-'
      return new Date(timestamp).toLocaleString('zh-CN')
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
        alertRuleId: knowledge.alertRuleId,
        title: knowledge.title,
        description: knowledge.description,
        steps: knowledge.steps.map((step, index) => ({
          description: step.description,
          linuxCommand: step.linuxCommand,
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
        alertRuleId: null,
        title: '',
        description: '',
        steps: []
      }
    },
    
    addStep() {
      this.currentKnowledge.steps.push({
        description: '',
        linuxCommand: '',
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
      if (!this.currentKnowledge.alertRuleId) {
        ElMessage.warning('请选择关联的告警规则')
        return
      }
      
      if (!this.currentKnowledge.title) {
        ElMessage.warning('请输入标题')
        return
      }
      
      // Validate steps
      for (let i = 0; i < this.currentKnowledge.steps.length; i++) {
        const step = this.currentKnowledge.steps[i]
        if (!step.description || !step.linuxCommand) {
          alert(`步骤 ${i + 1} 的描述和命令不能为空`)
          return
        }
      }
      
      // Prepare data for backend
      const payload = {
        alertRuleId: this.currentKnowledge.alertRuleId,
        title: this.currentKnowledge.title,
        description: this.currentKnowledge.description,
        steps: this.currentKnowledge.steps.map((step, index) => ({
          stepOrder: index + 1,
          description: step.description,
          linuxCommand: step.linuxCommand,
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
</style>
