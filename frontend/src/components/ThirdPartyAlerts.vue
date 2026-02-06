<template>
  <div class="third-party-alerts">
    <!-- Statistics Cards -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6" v-for="stat in statistics" :key="stat.channelCode">
        <el-card shadow="hover" :class="['stat-card', stat.enabled ? '' : 'disabled']">
          <div class="stat-header">
            <span class="channel-name">{{ stat.channelName }}</span>
            <el-tag :type="stat.enabled ? 'success' : 'info'" size="small">
              {{ stat.enabled ? '已启用' : '已禁用' }}
            </el-tag>
          </div>
          <div class="stat-content">
            <div class="stat-item">
              <span class="label">总推送:</span>
              <span class="value total">{{ stat.totalAlerts }}</span>
            </div>
            <div class="stat-item">
              <span class="label">成功:</span>
              <span class="value success">{{ stat.successCount }}</span>
            </div>
            <div class="stat-item">
              <span class="label">失败:</span>
              <span class="value failed">{{ stat.failedCount }}</span>
            </div>
          </div>
          <div class="stat-footer">
            <el-button type="primary" size="small" @click="viewChannelAlerts(stat.channelCode)" :icon="View">
              查看记录
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Channel Management -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>渠道配置</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索渠道名称或编码"
              :prefix-icon="Search"
              style="width: 250px; margin-right: 10px;"
              clearable
            />
            <el-button type="warning" @click="showMockDialog" :icon="MagicStick">
              Mock测试
            </el-button>
            <el-button type="primary" @click="showAddDialog" :icon="Plus">
              添加渠道
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredChannels" stripe style="width: 100%">
        <el-table-column prop="channelCode" label="渠道编码" width="150" />
        <el-table-column prop="channelName" label="渠道名称" width="200" />
        <el-table-column prop="description" label="描述" min-width="250" />
        <el-table-column prop="contactInfo" label="联系方式" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.enabled"
              @change="updateChannelStatus(scope.row)"
              active-color="#13ce66"
              inactive-color="#ff4949"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="editChannel(scope.row)" :icon="Edit">
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="deleteChannel(scope.row)" :icon="Delete">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Channel Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="channelForm" label-width="100px" ref="channelFormRef">
        <el-form-item label="渠道编码" required>
          <el-input
            v-model="channelForm.channelCode"
            placeholder="唯一标识，如: CHANNEL_001"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="渠道名称" required>
          <el-input v-model="channelForm.channelName" placeholder="渠道显示名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="channelForm.description"
            type="textarea"
            :rows="3"
            placeholder="渠道描述信息"
          />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="channelForm.contactInfo" placeholder="联系人或联系方式" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="channelForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveChannel">保存</el-button>
      </template>
    </el-dialog>

    <!-- Channel Alerts Dialog -->
    <el-dialog
      v-model="alertsDialogVisible"
      :title="`${currentChannelCode} - 告警记录`"
      width="80%"
      :close-on-click-modal="false"
    >
      <el-table :data="channelAlerts" stripe style="width: 100%" max-height="500">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="externalAlertId" label="外部ID" width="120" />
        <el-table-column prop="alertContent" label="告警内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.alertStatus === 'OPEN' ? 'danger' : 'success'" size="small">
              {{ scope.row.alertStatus === 'OPEN' ? '打开' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="100">
          <template #default="scope">
            <el-tag
              :type="
                scope.row.severity === 'CRITICAL'
                  ? 'danger'
                  : scope.row.severity === 'WARNING'
                  ? 'warning'
                  : 'info'
              "
              size="small"
            >
              {{ scope.row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="推送状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.pushStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ scope.row.pushStatus === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failureReason" label="失败原因" width="200" show-overflow-tooltip />
        <el-table-column prop="sourceIp" label="来源IP" width="120" />
        <el-table-column label="接收时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.receivedTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- Mock Test Dialog -->
    <el-dialog
      v-model="mockDialogVisible"
      title="Mock 告警测试"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="mockForm" label-width="120px">
        <el-form-item label="渠道选择" required>
          <el-select v-model="mockForm.channelCode" placeholder="请选择渠道" style="width: 100%">
            <el-option
              v-for="channel in channels"
              :key="channel.channelCode"
              :label="channel.channelName + ' (' + channel.channelCode + ')'"
              :value="channel.channelCode"
              :disabled="!channel.enabled"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="告警内容" required>
          <el-input
            v-model="mockForm.alertContent"
            type="textarea"
            :rows="4"
            placeholder="请输入模拟告警内容"
          />
        </el-form-item>
        <el-form-item label="外部告警ID" required>
          <el-input
            v-model="mockForm.externalAlertId"
            placeholder="必填，如：MOCK-2024-001"
          />
        </el-form-item>
        <el-form-item label="告警状态" required>
          <el-radio-group v-model="mockForm.alertStatus">
            <el-radio label="OPEN">打开</el-radio>
            <el-radio label="CLOSED">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="告警等级" required>
          <el-radio-group v-model="mockForm.severity">
            <el-radio label="通知">通知</el-radio>
            <el-radio label="提醒">提醒</el-radio>
            <el-radio label="一般">一般</el-radio>
            <el-radio label="重要">重要</el-radio>
            <el-radio label="致命">致命</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属应用" required>
          <el-select v-model="mockForm.appCode" placeholder="请选择所属应用" style="width: 100%">
            <el-option
              v-for="app in apps"
              :key="app.appCode"
              :label="app.appName + ' (' + app.appCode + ')'"
              :value="app.appCode"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendMockAlert">发送Mock告警</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Connection, Plus, Edit, Delete, View, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'ThirdPartyAlerts',
  components: {
    Connection,
    Plus,
    Edit,
    Delete,
    View,
    MagicStick
  },
  data() {
    return {
      statistics: [],
      channels: [],
      channelAlerts: [],
      apps: [],
      dialogVisible: false,
      alertsDialogVisible: false,
      mockDialogVisible: false,
      isEdit: false,
      searchKeyword: '',
      currentChannelCode: '',
      channelForm: {
        channelCode: '',
        channelName: '',
        description: '',
        contactInfo: '',
        enabled: true
      },
      mockForm: {
        channelCode: '',
        alertContent: '',
        externalAlertId: '',
        alertStatus: 'OPEN',
        severity: '提醒',
        appCode: ''
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑渠道' : '添加渠道'
    },
    filteredChannels() {
      if (!this.searchKeyword) {
        return this.channels
      }
      const keyword = this.searchKeyword.toLowerCase()
      return this.channels.filter(channel =>
        channel.channelName.toLowerCase().includes(keyword) ||
        channel.channelCode.toLowerCase().includes(keyword) ||
        (channel.description && channel.description.toLowerCase().includes(keyword))
      )
    }
  },
  mounted() {
    this.loadStatistics()
    this.loadChannels()
    this.loadApps()
  },
  methods: {
    async loadStatistics() {
      try {
        const response = await fetch('/api/third-party-alerts/statistics')
        this.statistics = await response.json()
      } catch (error) {
        console.error('Error loading statistics:', error)
        ElMessage.error('加载统计信息失败')
      }
    },

    async loadChannels() {
      try {
        const response = await fetch('/api/alert-channels')
        this.channels = await response.json()
      } catch (error) {
        console.error('Error loading channels:', error)
        ElMessage.error('加载渠道列表失败')
      }
    },

    async loadApps() {
      try {
        const response = await fetch('/api/apps')
        const result = await response.json()
        this.apps = result.data || []
      } catch (error) {
        console.error('Error loading apps:', error)
      }
    },

    async viewChannelAlerts(channelCode) {
      this.currentChannelCode = channelCode
      try {
        const response = await fetch(`/api/third-party-alerts/channel/${channelCode}`)
        this.channelAlerts = await response.json()
        this.alertsDialogVisible = true
      } catch (error) {
        console.error('Error loading channel alerts:', error)
        ElMessage.error('加载告警记录失败')
      }
    },

    showAddDialog() {
      this.isEdit = false
      this.channelForm = {
        channelCode: '',
        channelName: '',
        description: '',
        contactInfo: '',
        enabled: true
      }
      this.dialogVisible = true
    },

    editChannel(channel) {
      this.isEdit = true
      this.channelForm = { ...channel }
      this.dialogVisible = true
    },

    async saveChannel() {
      if (!this.channelForm.channelCode || !this.channelForm.channelName) {
        ElMessage.warning('请填写必填项')
        return
      }

      try {
        const url = this.isEdit
          ? `/api/alert-channels/${this.channelForm.id}`
          : '/api/alert-channels'
        const method = this.isEdit ? 'PUT' : 'POST'

        const response = await fetch(url, {
          method: method,
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.channelForm)
        })

        if (response.ok) {
          ElMessage.success(this.isEdit ? '更新成功' : '添加成功')
          this.dialogVisible = false
          await this.loadChannels()
          await this.loadStatistics()
        } else if (response.status === 409) {
          ElMessage.error('渠道编码已存在')
        } else {
          ElMessage.error('操作失败')
        }
      } catch (error) {
        console.error('Error saving channel:', error)
        ElMessage.error('操作失败')
      }
    },

    async updateChannelStatus(channel) {
      try {
        const response = await fetch(`/api/alert-channels/${channel.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(channel)
        })

        if (response.ok) {
          ElMessage.success('状态更新成功')
          await this.loadStatistics()
        } else {
          ElMessage.error('状态更新失败')
          channel.enabled = !channel.enabled
        }
      } catch (error) {
        console.error('Error updating channel status:', error)
        ElMessage.error('状态更新失败')
        channel.enabled = !channel.enabled
      }
    },

    async deleteChannel(channel) {
      try {
        await ElMessageBox.confirm(`确定要删除渠道 "${channel.channelName}" 吗？`, '确认删除', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const response = await fetch(`/api/alert-channels/${channel.id}`, {
          method: 'DELETE'
        })

        if (response.ok) {
          ElMessage.success('删除成功')
          await this.loadChannels()
          await this.loadStatistics()
        } else {
          ElMessage.error('删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('Error deleting channel:', error)
          ElMessage.error('删除失败')
        }
      }
    },

    formatTime(timestamp) {
      if (!timestamp) return '-'
      // Handle both ISO string and milliseconds
      const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp)
      return date.toLocaleString('zh-CN')
    },

    showMockDialog() {
      this.mockDialogVisible = true
      this.mockForm = {
        channelCode: this.channels.length > 0 ? this.channels[0].channelCode : '',
        alertContent: '',
        externalAlertId: '',
        alertStatus: 'OPEN',
        severity: '提醒',
        appCode: this.apps.length > 0 ? this.apps[0].appCode : ''
      }
    },

    async sendMockAlert() {
      if (!this.mockForm.channelCode || !this.mockForm.alertContent || !this.mockForm.externalAlertId || !this.mockForm.appCode) {
        ElMessage.warning('请填写所有必填项')
        return
      }

      try {
        const response = await fetch('/api/third-party-alerts/push', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.mockForm)
        })

        const result = await response.json()

        if (result.success) {
          ElMessage.success('Mock告警发送成功')
          this.mockDialogVisible = false
          // 刷新统计信息
          await this.loadStatistics()
        } else {
          ElMessage.error('发送失败: ' + result.message)
        }
      } catch (error) {
        console.error('Error sending mock alert:', error)
        ElMessage.error('发送失败')
      }
    },

    goBack() {
      this.$router.back()
    }
  }
}
</script>

<style scoped>
.third-party-alerts {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-card {
  margin-bottom: 20px;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card.disabled {
  opacity: 0.6;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.channel-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 15px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-item .label {
  color: #666;
  font-size: 14px;
}

.stat-item .value {
  font-size: 20px;
  font-weight: 600;
}

.stat-item .value.total {
  color: #409eff;
}

.stat-item .value.success {
  color: #67c23a;
}

.stat-item .value.failed {
  color: #f56c6c;
}

.stat-footer {
  text-align: center;
}
</style>
