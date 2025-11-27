<template>
  <div class="agent-management">
    <div class="section-header">
      <h2><span class="icon">🖥️</span> 代理管理</h2>
    </div>
    
    <div class="agent-form card">
      <h3>添加新代理</h3>
      <form @submit.prevent="addAgent">
        <div class="form-row">
          <div class="form-group">
            <label>名称：</label>
            <input v-model="newAgent.name" required placeholder="输入代理名称" />
          </div>
          <div class="form-group">
            <label>IP地址：</label>
            <input v-model="newAgent.ip" required placeholder="例如: 192.168.1.100" />
          </div>
          <div class="form-group">
            <label>端口：</label>
            <input v-model.number="newAgent.port" type="number" required placeholder="8081" />
          </div>
          <button type="submit" class="btn-primary">添加代理</button>
        </div>
      </form>
    </div>
    
    <div class="agent-list card">
      <h3>代理列表</h3>
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>IP地址</th>
              <th>端口</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="agent in agents" :key="agent.id">
              <td>{{ agent.id }}</td>
              <td>{{ agent.name }}</td>
              <td>{{ agent.ip }}</td>
              <td>{{ agent.port }}</td>
              <td><span :class="'status status-' + agent.status.toLowerCase()">{{ getStatusText(agent.status) }}</span></td>
              <td>
                <button @click="deleteAgent(agent.id)" class="btn-danger">删除</button>
              </td>
            </tr>
            <tr v-if="agents.length === 0">
              <td colspan="6" class="no-data">暂无代理数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AgentManagement',
  data() {
    return {
      agents: [],
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
      try {
        const response = await fetch('/api/agents')
        this.agents = await response.json()
      } catch (error) {
        console.error('Error loading agents:', error)
      }
    },
    
    async addAgent() {
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
        }
      } catch (error) {
        console.error('Error adding agent:', error)
      }
    },
    
    async deleteAgent(id) {
      if (!confirm('确定要删除该代理吗？')) return
      
      try {
        const response = await fetch(`/api/agents/${id}`, {
          method: 'DELETE'
        })
        
        if (response.ok) {
          this.agents = this.agents.filter(agent => agent.id !== id)
        }
      } catch (error) {
        console.error('Error deleting agent:', error)
      }
    },
    
    getStatusText(status) {
      const statusMap = {
        'ACTIVE': '活跃',
        'INACTIVE': '非活跃',
        'DISCONNECTED': '断开'
      }
      return statusMap[status] || status
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

.form-group input {
  padding: 10px 15px;
  background: #ffffff;
  border: 2px solid #e3f2fd;
  border-radius: 6px;
  color: #1976d2;
  font-size: 14px;
  min-width: 200px;
  transition: all 0.3s ease;
}

.form-group input:focus {
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

.status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-active {
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #66bb6a;
}

.status-inactive {
  background: #fff3e0;
  color: #ef6c00;
  border: 1px solid #ffa726;
}

.status-disconnected {
  background: #ffebee;
  color: #c62828;
  border: 1px solid #ef5350;
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