<template>
  <div class="emergency-knowledge">
    <div class="section-header">
      <h2><span class="icon">📚</span> 应急知识库</h2>
    </div>
    
    <!-- Knowledge List -->
    <div class="card">
      <div class="card-header">
        <h3>知识库列表</h3>
        <button @click="openCreateDialog" class="btn-primary">➕ 新建知识库</button>
      </div>
      
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>标题</th>
              <th>关联告警规则</th>
              <th>步骤数量</th>
              <th>创建时间</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="knowledge in knowledgeList" :key="knowledge.id">
              <td>{{ knowledge.id }}</td>
              <td>{{ knowledge.title }}</td>
              <td>{{ getAlertRuleName(knowledge.alertRuleId) }}</td>
              <td><span class="step-count">{{ knowledge.steps ? knowledge.steps.length : 0 }} 步</span></td>
              <td>{{ formatTime(knowledge.createdAt) }}</td>
              <td>{{ formatTime(knowledge.updatedAt) }}</td>
              <td>
                <button @click="openEditDialog(knowledge)" class="btn-info">编辑</button>
                <button @click="viewKnowledge(knowledge)" class="btn-view">查看</button>
                <button @click="deleteKnowledge(knowledge.id)" class="btn-danger">删除</button>
              </td>
            </tr>
            <tr v-if="knowledgeList.length === 0">
              <td colspan="7" class="no-data">暂无知识库数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    
    <!-- Create/Edit Dialog -->
    <div v-if="showDialog" class="dialog-overlay" @click.self="closeDialog">
      <div class="dialog-content">
        <div class="dialog-header">
          <h3>{{ isEditMode ? '📝 编辑知识库' : '➕ 新建知识库' }}</h3>
          <button @click="closeDialog" class="btn-close">&times;</button>
        </div>
        
        <div class="dialog-body">
          <div class="form-group">
            <label>关联告警规则：</label>
            <select v-model="currentKnowledge.alertRuleId" :disabled="isEditMode" required>
              <option :value="null">请选择告警规则</option>
              <option v-for="rule in availableAlertRules" :key="rule.id" :value="rule.id">
                {{ rule.name }}
              </option>
            </select>
          </div>
          
          <div class="form-group">
            <label>标题：</label>
            <input v-model="currentKnowledge.title" placeholder="输入应急处理标题" required />
          </div>
          
          <div class="form-group">
            <label>描述：</label>
            <textarea v-model="currentKnowledge.description" rows="3" placeholder="输入整体描述"></textarea>
          </div>
          
          <div class="steps-section">
            <div class="steps-header">
              <h4>应急操作步骤</h4>
              <button @click="addStep" class="btn-add-step">➕ 添加步骤</button>
            </div>
            
            <div v-if="currentKnowledge.steps.length === 0" class="no-steps">
              暂无操作步骤，请添加
            </div>
            
            <div v-for="(step, index) in currentKnowledge.steps" :key="index" class="step-item">
              <div class="step-header">
                <span class="step-number">步骤 {{ index + 1 }}</span>
                <div class="step-actions">
                  <button v-if="index > 0" @click="moveStepUp(index)" class="btn-move" title="上移">⬆</button>
                  <button v-if="index < currentKnowledge.steps.length - 1" @click="moveStepDown(index)" class="btn-move" title="下移">⬇</button>
                  <button @click="removeStep(index)" class="btn-remove-step">&times;</button>
                </div>
              </div>
              
              <div class="step-content">
                <div class="form-group">
                  <label>步骤描述：</label>
                  <input v-model="step.description" placeholder="请描述该步骤的目的" required />
                </div>
                
                <div class="form-group">
                  <label>Linux命令：</label>
                  <textarea v-model="step.linuxCommand" rows="2" placeholder="输入要执行的Linux命令" class="command-input" required></textarea>
                </div>
                
                <div class="form-row-inline">
                  <div class="form-group">
                    <label>依赖步骤：</label>
                    <select v-model="step.dependsOnIndex">
                      <option :value="null">无依赖</option>
                      <option v-for="i in index" :key="i" :value="i - 1">步骤 {{ i }}</option>
                    </select>
                  </div>
                  
                  <div class="form-group">
                    <label>备注：</label>
                    <input v-model="step.notes" placeholder="额外说明或警告" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="dialog-footer">
          <button @click="saveKnowledge" class="btn-primary">保存</button>
          <button @click="closeDialog" class="btn-secondary">取消</button>
        </div>
      </div>
    </div>
    
    <!-- View Dialog -->
    <div v-if="showViewDialog" class="dialog-overlay" @click.self="closeViewDialog">
      <div class="dialog-content view-dialog">
        <div class="dialog-header">
          <h3>📖 查看知识库</h3>
          <button @click="closeViewDialog" class="btn-close">&times;</button>
        </div>
        
        <div class="dialog-body">
          <div class="view-section">
            <h4>{{ viewingKnowledge.title }}</h4>
            <p v-if="viewingKnowledge.description" class="description">{{ viewingKnowledge.description }}</p>
            <div class="meta-info">
              <span>关联规则: {{ getAlertRuleName(viewingKnowledge.alertRuleId) }}</span>
              <span>更新时间: {{ formatTime(viewingKnowledge.updatedAt) }}</span>
            </div>
          </div>
          
          <div class="steps-view">
            <h4>应急操作步骤</h4>
            <div v-for="(step, index) in viewingKnowledge.steps" :key="index" class="step-view-item">
              <div class="step-view-header">
                <span class="step-number-large">{{ index + 1 }}</span>
                <h5>{{ step.description }}</h5>
              </div>
              <div class="step-view-content">
                <div class="command-view">
                  <div class="command-label">执行命令：</div>
                  <pre class="command-text">{{ step.linuxCommand }}</pre>
                </div>
                <div v-if="step.dependsOn" class="dependency-info">
                  ⚠️ 依赖步骤: 步骤 {{ findStepNumber(viewingKnowledge.steps, step.dependsOn) }}
                </div>
                <div v-if="step.notes" class="notes-info">
                  💡 {{ step.notes }}
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="dialog-footer">
          <button @click="closeViewDialog" class="btn-secondary">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EmergencyKnowledge',
  data() {
    return {
      knowledgeList: [],
      alertRules: [],
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
      try {
        // Load all knowledge
        const knowledgeResponse = await fetch('/api/emergency')
        this.knowledgeList = await knowledgeResponse.json()
        
        // Load alert rules
        const rulesResponse = await fetch('/api/alerts/rules')
        this.alertRules = await rulesResponse.json()
      } catch (error) {
        console.error('Error loading data:', error)
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
      // Check if any later steps depend on this one
      const hasDependents = this.currentKnowledge.steps.some(
        (step, i) => i > index && step.dependsOnIndex === index
      )
      
      if (hasDependents) {
        if (!confirm('有其他步骤依赖于该步骤，确定要删除吗？')) {
          return
        }
      }
      
      this.currentKnowledge.steps.splice(index, 1)
      
      // Update dependency indices for remaining steps
      this.currentKnowledge.steps.forEach(step => {
        if (step.dependsOnIndex !== null && step.dependsOnIndex > index) {
          step.dependsOnIndex--
        } else if (step.dependsOnIndex === index) {
          step.dependsOnIndex = null
        }
      })
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
        alert('请选择关联的告警规则')
        return
      }
      
      if (!this.currentKnowledge.title) {
        alert('请输入标题')
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
          alert('保存成功')
          this.closeDialog()
          this.loadData()
        } else {
          alert('保存失败')
        }
      } catch (error) {
        console.error('Error saving knowledge:', error)
        alert('保存失败')
      }
    },
    
    async deleteKnowledge(id) {
      if (!confirm('确定要删除该知识库吗？')) return
      
      try {
        const response = await fetch(`/api/emergency/${id}`, {
          method: 'DELETE'
        })
        
        if (response.ok) {
          alert('删除成功')
          this.loadData()
        } else {
          alert('删除失败')
        }
      } catch (error) {
        console.error('Error deleting knowledge:', error)
        alert('删除失败')
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card h3 {
  color: #1976d2;
  font-size: 18px;
  margin: 0;
  border-bottom: none;
  padding-bottom: 0;
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

.step-count {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  background: #e3f2fd;
  color: #1976d2;
  border: 1px solid #64b5f6;
}

.btn-primary {
  padding: 10px 20px;
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
  box-shadow: 0 5px 15px rgba(25, 118, 210, 0.3);
}

.btn-info {
  padding: 6px 16px;
  background: #1976d2;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-right: 5px;
}

.btn-info:hover {
  background: #1565c0;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(25, 118, 210, 0.3);
}

.btn-view {
  padding: 6px 16px;
  background: #4caf50;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-right: 5px;
}

.btn-view:hover {
  background: #43a047;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(76, 175, 80, 0.3);
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

/* Dialog Styles */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 12px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(25, 118, 210, 0.3);
  border: 2px solid #1976d2;
}

.view-dialog {
  max-width: 800px;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px;
  background: linear-gradient(135deg, #1976d2, #1565c0);
  color: white;
}

.dialog-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: white;
}

.btn-close {
  background: none;
  border: none;
  color: white;
  font-size: 32px;
  cursor: pointer;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.btn-close:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: rotate(90deg);
}

.dialog-body {
  padding: 25px;
  overflow-y: auto;
  flex: 1;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  color: #1976d2;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 10px 15px;
  background: #ffffff;
  border: 2px solid #e3f2fd;
  border-radius: 6px;
  color: #000000;
  font-size: 14px;
  transition: all 0.3s ease;
  font-family: inherit;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 10px rgba(25, 118, 210, 0.2);
}

.command-input {
  font-family: 'Courier New', monospace;
  background: #f5f5f5 !important;
}

.steps-section {
  margin-top: 25px;
  padding-top: 25px;
  border-top: 2px solid #e3f2fd;
}

.steps-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.steps-header h4 {
  color: #1976d2;
  font-size: 16px;
  margin: 0;
  font-weight: 600;
}

.btn-add-step {
  padding: 8px 16px;
  background: #4caf50;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 600;
}

.btn-add-step:hover {
  background: #43a047;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(76, 175, 80, 0.3);
}

.no-steps {
  text-align: center;
  color: #9e9e9e;
  font-style: italic;
  padding: 40px;
  background: #f5f5f5;
  border-radius: 8px;
  border: 2px dashed #e0e0e0;
}

.step-item {
  background: #f5f9ff;
  border: 2px solid #e3f2fd;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.step-number {
  color: #1976d2;
  font-weight: 600;
  font-size: 16px;
}

.step-actions {
  display: flex;
  gap: 5px;
}

.btn-move {
  background: #2196f3;
  border: none;
  color: white;
  font-size: 16px;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-move:hover {
  background: #1976d2;
}

.btn-remove-step {
  background: #f44336;
  border: none;
  color: white;
  font-size: 24px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.btn-remove-step:hover {
  background: #d32f2f;
  transform: rotate(90deg);
}

.step-content {
  padding-left: 10px;
}

.form-row-inline {
  display: flex;
  gap: 15px;
}

.form-row-inline .form-group {
  flex: 1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px 25px;
  background: #f5f5f5;
  border-top: 2px solid #e3f2fd;
}

.btn-secondary {
  padding: 10px 24px;
  background: #757575;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: #616161;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(117, 117, 117, 0.3);
}

/* View Dialog Styles */
.view-section {
  margin-bottom: 25px;
}

.view-section h4 {
  color: #1976d2;
  font-size: 20px;
  margin-bottom: 10px;
  font-weight: 600;
}

.view-section .description {
  color: #000000;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 15px;
}

.meta-info {
  display: flex;
  gap: 20px;
  color: #666;
  font-size: 13px;
}

.steps-view h4 {
  color: #1976d2;
  font-size: 18px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e3f2fd;
  font-weight: 600;
}

.step-view-item {
  background: #f5f9ff;
  border: 2px solid #e3f2fd;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
}

.step-view-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.step-number-large {
  background: #1976d2;
  color: white;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-view-header h5 {
  color: #1976d2;
  font-size: 16px;
  margin: 0;
  font-weight: 600;
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

.dependency-info {
  background: #fff3e0;
  border: 1px solid #ff9800;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #e65100;
}

.notes-info {
  background: #e3f2fd;
  border: 1px solid #1976d2;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: #000000;
}
</style>
