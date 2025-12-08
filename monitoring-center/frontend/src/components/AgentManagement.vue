<template>
  <div class="agent-management">
    
    <el-card class="agent-form" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>添加新代理</span>
        </div>
      </template>
      <el-form :model="newAgent" label-width="100px" :inline="true" @submit.prevent="addAgent">
        <el-form-item label="名称">
          <el-input v-model="newAgent.name" placeholder="输入代理名称" required style="width: 200px" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="newAgent.ip" placeholder="例如: 192.168.1.100" required style="width: 200px" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="newAgent.port" :min="1" :max="65535" required style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addAgent" :icon="Plus">添加代理</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="agent-list" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>代理列表</span>
        </div>
      </template>
      <el-table :data="agents" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="ip" label="IP地址" min-width="150" />
        <el-table-column prop="port" label="端口" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
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
    </el-card>
  </div>
</template>

<script>
import { Monitor, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'AgentManagement',
  components: {
    Monitor,
    Plus,
    Delete
  },
  data() {
    return {
      agents: [],
      loading: false,
      newAgent: {
        name: '',
        ip: '',
        port: 8080
      }
    }
  },
  mounted() {
    this.loadAgents()
  },
  methods: {
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
    
    async addAgent() {
      if (!this.newAgent.name || !this.newAgent.ip || !this.newAgent.port) {
        ElMessage.warning('请填写完整信息')
        return
      }
      
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
          this.newAgent = { name: '', ip: '', port: 8080 }
          ElMessage.success('代理添加成功')
        } else {
          ElMessage.error('代理添加失败')
        }
      } catch (error) {
        console.error('Error adding agent:', error)
        ElMessage.error('代理添加失败')
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

.agent-form,
.agent-list {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}
</style>