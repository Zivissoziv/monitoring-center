<template>
  <div class="agent-management">
    
    <el-alert type="success" :closable="false" style="margin-bottom: 20px;">
      <template #title>
        <strong>提示</strong>
      </template>
      新增代理后，系统会自动为其分配所有启用的指标。如需自定义配置，请前往<strong>「指标定义」</strong>页面，展开对应指标进行调整。
    </el-alert>
    
    <!-- Search and Filter Bar -->
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="名称">
          <el-input v-model="searchForm.name" placeholder="代理名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="searchForm.ip" placeholder="IP地址" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 150px">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="非活跃" value="INACTIVE" />
            <el-option label="断开" value="DISCONNECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="agent-list" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>代理列表</span>
          <div>
            <el-button 
              type="success" 
              @click="pushConfigToAllAgents" 
              :icon="Upload"
              :loading="pushingAll"
              style="margin-right: 10px"
            >
              推送配置到所有代理
            </el-button>
            <el-button type="primary" @click="showAddDialog" :icon="Plus">添加代理</el-button>
          </div>
        </div>
      </template>
      <el-table :data="paginatedAgents" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="ip" label="IP地址" min-width="150" />
        <el-table-column prop="port" label="端口" width="100" />
        <el-table-column prop="appCode" label="所属应用" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.appCode" type="primary" size="small">
              {{ getAppName(scope.row.appCode) }}
            </el-tag>
            <span v-else style="color: #909399">无</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="scope">
            <el-button 
              size="small" 
              type="primary" 
              :icon="Upload"
              @click="pushConfigToAgent(scope.row.id)"
              :loading="pushingConfig[scope.row.id]"
            >
              推送配置
            </el-button>
            <el-popconfirm 
              title="确定要删除该代理吗？" 
              @confirm="deleteAgent(scope.row.id)"
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
          <el-empty description="暂无代理数据" />
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
    
    <!-- Add Agent Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="添加新代理"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="newAgent" label-width="100px" ref="agentForm">
        <el-form-item label="Agent ID" required>
          <el-input 
            v-model="newAgent.id" 
            placeholder="与agent配置中的agent.name保持一致，如: Server1-Agent" 
          />
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            提示：必须与agent配置文件中的agent.name完全一致
          </div>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="newAgent.name" placeholder="输入代理显示名称" />
        </el-form-item>
        <el-form-item label="IP地址" required>
          <el-input v-model="newAgent.ip" placeholder="例如: 192.168.1.100" />
        </el-form-item>
        <el-form-item label="端口" required>
          <el-input-number v-model="newAgent.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="所属应用" required>
          <el-select v-model="newAgent.appCode" placeholder="请选择所属应用" style="width: 100%">
            <el-option 
              v-for="app in apps" 
              :key="app.appCode" 
              :label="`${app.appCode} - ${app.appName}`" 
              :value="app.appCode" 
            />
          </el-select>
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            提示：代理必须属于一个应用，该应用的用户才能查看该代理上送的数据
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="warning" 
            @click="testConnection" 
            :loading="testing"
            :icon="Connection"
            style="width: 100%"
          >
            {{ testing ? '测试中...' : '测试连接' }}
          </el-button>
        </el-form-item>
        
        <el-alert 
          v-if="testResult.show"
          :type="testResult.type"
          :title="testResult.message"
          :closable="false"
          style="margin-bottom: 15px"
        />
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addAgent" :loading="adding">
            {{ adding ? '添加中...' : '确定添加' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Monitor, Plus, Delete, Connection, Search, Refresh, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'AgentManagement',
  components: {
    Monitor,
    Plus,
    Delete,
    Connection,
    Search,
    Refresh,
    Upload
  },
  data() {
    return {
      agents: [],
      apps: [],
      loading: false,
      dialogVisible: false,
      testing: false,
      adding: false,
      pushingAll: false,
      pushingConfig: {}, // Track loading state for each agent
      searchForm: {
        name: '',
        ip: '',
        status: ''
      },
      pagination: {
        currentPage: 1,
        pageSize: 10
      },
      testResult: {
        show: false,
        type: 'info',
        message: '',
        success: false
      },
      newAgent: {
        id: '',
        name: '',
        ip: '',
        port: 8080,
        appCode: ''
      }
    }
  },
  computed: {
    filteredAgents() {
      let filtered = this.agents
      
      if (this.searchForm.name) {
        filtered = filtered.filter(agent => 
          agent.name.toLowerCase().includes(this.searchForm.name.toLowerCase())
        )
      }
      
      if (this.searchForm.ip) {
        filtered = filtered.filter(agent => 
          agent.ip.includes(this.searchForm.ip)
        )
      }
      
      if (this.searchForm.status) {
        filtered = filtered.filter(agent => 
          agent.status === this.searchForm.status
        )
      }
      
      return filtered
    },
    filteredTotal() {
      return this.filteredAgents.length
    },
    paginatedAgents() {
      const start = (this.pagination.currentPage - 1) * this.pagination.pageSize
      const end = start + this.pagination.pageSize
      return this.filteredAgents.slice(start, end)
    }
  },
  mounted() {
    this.loadAgents()
    this.loadApps()
  },
  methods: {
    showAddDialog() {
      this.dialogVisible = true
      this.newAgent = { id: '', name: '', ip: '', port: 8080, appCode: '' }
      this.testResult = { show: false, type: 'info', message: '', success: false }
    },
    
    async testConnection() {
      if (!this.newAgent.ip || !this.newAgent.port) {
        ElMessage.warning('请先填写IP地址和端口')
        return
      }
      
      this.testing = true
      this.testResult.show = false
      
      try {
        // Use backend API to test connection (avoid CORS issues)
        const response = await fetch('/api/agents/connection-test', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            ip: this.newAgent.ip,
            port: this.newAgent.port
          })
        })
        
        if (response.ok) {
          const data = await response.json()
          
          if (data.success) {
            this.testResult = {
              show: true,
              type: 'success',
              message: `连接成功！代理状态: ${data.status}`,
              success: true
            }
            ElMessage.success('代理连接测试成功')
          } else {
            this.testResult = {
              show: true,
              type: 'error',
              message: `连接失败！${data.message || '无法连接到代理'}`,
              success: false
            }
            ElMessage.error('代理连接测试失败')
          }
        } else {
          this.testResult = {
            show: true,
            type: 'error',
            message: `连接失败！HTTP状态码: ${response.status}`,
            success: false
          }
          ElMessage.error('代理连接测试失败')
        }
      } catch (error) {
        this.testResult = {
          show: true,
          type: 'error',
          message: `连接失败！错误: ${error.message || '网络错误'}`,
          success: false
        }
        ElMessage.error('代理连接测试失败: ' + (error.message || '超时或网络错误'))
      } finally {
        this.testing = false
      }
    },
    async loadAgents() {
      this.loading = true
      try {
        const response = await fetch('/api/agents')
        this.agents = await response.json()
      } catch (error) {
        console.error('Error loading agents:', error)
        ElMessage.error('加载代理列表失败')
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
        ip: '',
        status: ''
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
    
    async addAgent() {
      if (!this.newAgent.id || !this.newAgent.name || !this.newAgent.ip || !this.newAgent.port || !this.newAgent.appCode) {
        ElMessage.warning('请填写完整信息（包括所属应用）')
        return
      }
      
      this.adding = true
      
      try {
        const response = await fetch('/api/agents', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.newAgent)
        })
        
        if (response.ok) {
          const agent = await response.json()
          this.agents.push(agent)
          ElMessage.success('代理添加成功')
          this.dialogVisible = false
          this.newAgent = { id: '', name: '', ip: '', port: 8080, appCode: '' }
          this.testResult = { show: false, type: 'info', message: '', success: false }
          
          // Reload agents to get updated data
          await this.loadAgents()
          
          // Auto push configuration to the new agent
          ElMessage.info('正在推送采集配置到新代理...')
          try {
            await fetch(`/api/agents/${agent.id}/config`, {
              method: 'POST'
            })
            ElMessage.success('采集配置已推送到代理')
          } catch (error) {
            console.error('Error pushing config to new agent:', error)
            ElMessage.warning('配置推送失败，请手动点击“推送配置”按钮')
          }
        } else {
          const errorText = await response.text()
          console.error('Add agent failed:', response.status, errorText)
          ElMessage.error(`代理添加失败: ${response.status} ${errorText || ''}`)
        }
      } catch (error) {
        console.error('Error adding agent:', error)
        ElMessage.error('代理添加失败: ' + error.message)
      } finally {
        this.adding = false
      }
    },
    
    async deleteAgent(id) {
      try {
        const response = await fetch(`/api/agents/${id}`, {
          method: 'DELETE'
        })
        
        if (response.ok) {
          this.agents = this.agents.filter(agent => agent.id !== id)
          ElMessage.success('代理删除成功')
        } else {
          ElMessage.error('代理删除失败')
        }
      } catch (error) {
        console.error('Error deleting agent:', error)
        ElMessage.error('代理删除失败')
      }
    },
    
    getStatusText(status) {
      const statusMap = {
        'ACTIVE': '活跃',
        'INACTIVE': '非活跃',
        'DISCONNECTED': '断开'
      }
      return statusMap[status] || status
    },
    
    getStatusType(status) {
      const typeMap = {
        'ACTIVE': 'success',
        'INACTIVE': 'warning',
        'DISCONNECTED': 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    async pushConfigToAgent(agentId) {
      this.$set(this.pushingConfig, agentId, true)
      
      try {
        const response = await fetch(`/api/agents/${agentId}/config`, {
          method: 'POST'
        })
        
        if (response.ok) {
          ElMessage.success('配置推送成功')
        } else {
          const errorText = await response.text()
          ElMessage.error(`配置推送失败: ${errorText}`)
        }
      } catch (error) {
        console.error('Error pushing config:', error)
        ElMessage.error('配置推送失败: ' + error.message)
      } finally {
        this.$set(this.pushingConfig, agentId, false)
      }
    },
    
    async pushConfigToAllAgents() {
      this.pushingAll = true
      
      try {
        const response = await fetch('/api/agents/config/batch-push', {
          method: 'POST'
        })
        
        if (response.ok) {
          ElMessage.success('配置已推送到所有代理')
        } else {
          const errorText = await response.text()
          ElMessage.error(`配置推送失败: ${errorText}`)
        }
      } catch (error) {
        console.error('Error pushing config to all agents:', error)
        ElMessage.error('配置推送失败: ' + error.message)
      } finally {
        this.pushingAll = false
      }
    },
    
    async loadApps() {
      try {
        const response = await fetch('/api/apps/enabled')
        const result = await response.json()
        if (result.success) {
          this.apps = result.data || []
        }
      } catch (error) {
        console.error('Error loading apps:', error)
      }
    },
    
    getAppName(appCode) {
      const app = this.apps.find(a => a.appCode === appCode)
      return app ? `${appCode} - ${app.appName}` : appCode
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

.agent-management {
  padding: 20px;
}

.el-page-header {
  margin-bottom: 24px;
}

.agent-list {
  margin-bottom: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}
</style>