<template>
  <div class="app-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>应用管理</span>
          <el-button type="primary" @click="showAddDialog">
            <el-icon><Plus /></el-icon>
            新增应用
          </el-button>
        </div>
      </template>

      <el-table :data="apps" v-loading="loading" stripe>
        <el-table-column prop="appCode" label="应用编码" width="120" />
        <el-table-column prop="appName" label="应用名称" width="180" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="showUserBindDialog(row)">绑定用户</el-button>
            <el-button size="small" type="danger" @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑应用' : '新增应用'"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="应用编码" prop="appCode">
          <el-input 
            v-model="form.appCode" 
            :disabled="isEdit"
            placeholder="4位以内大写字母"
            maxlength="10"
            @input="form.appCode = form.appCode.toUpperCase()"
          />
          <div class="form-tip">应用编码为1-4位大写字母，如 CORE、APP1</div>
        </el-form-item>
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="form.appName" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入应用描述"
          />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- User Binding Dialog -->
    <el-dialog
      v-model="userBindDialogVisible"
      title="绑定用户"
      width="600px"
    >
      <div class="bind-dialog-content">
        <div class="bind-info">
          <span>应用：</span>
          <el-tag type="primary">{{ currentApp?.appCode }} - {{ currentApp?.appName }}</el-tag>
        </div>
        <el-transfer
          v-model="selectedUserIds"
          :data="allUsers"
          :titles="['未绑定用户', '已绑定用户']"
          :props="{ key: 'id', label: 'displayName' }"
          filterable
          filter-placeholder="搜索用户"
        />
      </div>
      <template #footer>
        <el-button @click="userBindDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUserBinding" :loading="bindSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

export default {
  name: 'AppManagement',
  components: { Plus },
  setup() {
    const loading = ref(false)
    const submitting = ref(false)
    const bindSaving = ref(false)
    const apps = ref([])
    const allUsers = ref([])
    const dialogVisible = ref(false)
    const userBindDialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    const currentApp = ref(null)
    const selectedUserIds = ref([])

    const form = reactive({
      id: null,
      appCode: '',
      appName: '',
      description: '',
      enabled: true
    })

    const rules = {
      appCode: [
        { required: true, message: '请输入应用编码', trigger: 'blur' },
        { pattern: /^[A-Z]{1,10}$/, message: '应用编码为1-10位大写字母', trigger: 'blur' }
      ],
      appName: [
        { required: true, message: '请输入应用名称', trigger: 'blur' }
      ]
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      const date = new Date(dateStr)
      return date.toLocaleString('zh-CN')
    }

    const loadApps = async () => {
      loading.value = true
      try {
        const response = await fetch('/api/apps')
        const result = await response.json()
        if (result.success) {
          apps.value = result.data || []
        }
      } catch (error) {
        console.error('Failed to load apps:', error)
        ElMessage.error('加载应用列表失败')
      } finally {
        loading.value = false
      }
    }

    const loadAllUsers = async () => {
      try {
        const response = await fetch('/api/users')
        const result = await response.json()
        if (result.success) {
          allUsers.value = (result.data || []).map(user => ({
            ...user,
            displayName: `${user.username} (${user.nickname || '-'})`
          }))
        }
      } catch (error) {
        console.error('Failed to load users:', error)
      }
    }

    const showAddDialog = () => {
      isEdit.value = false
      Object.assign(form, {
        id: null,
        appCode: '',
        appName: '',
        description: '',
        enabled: true
      })
      dialogVisible.value = true
    }

    const showEditDialog = (app) => {
      isEdit.value = true
      Object.assign(form, {
        id: app.id,
        appCode: app.appCode,
        appName: app.appName,
        description: app.description || '',
        enabled: app.enabled
      })
      dialogVisible.value = true
    }

    const showUserBindDialog = async (app) => {
      currentApp.value = app
      selectedUserIds.value = []
      
      // Load current app users
      try {
        const response = await fetch(`/api/apps/${app.appCode}/users`)
        const result = await response.json()
        if (result.success) {
          selectedUserIds.value = result.data || []
        }
      } catch (error) {
        console.error('Failed to load app users:', error)
      }
      
      userBindDialogVisible.value = true
    }

    const submitForm = async () => {
      if (!formRef.value) return
      
      try {
        await formRef.value.validate()
      } catch {
        return
      }

      submitting.value = true
      try {
        const url = isEdit.value ? `/api/apps/${form.id}` : '/api/apps'
        const method = isEdit.value ? 'PUT' : 'POST'
        
        const response = await fetch(url, {
          method,
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(form)
        })
        
        const result = await response.json()
        if (result.success) {
          ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
          dialogVisible.value = false
          loadApps()
        } else {
          ElMessage.error(result.message || '操作失败')
        }
      } catch (error) {
        console.error('Submit failed:', error)
        ElMessage.error('操作失败')
      } finally {
        submitting.value = false
      }
    }

    const confirmDelete = (app) => {
      ElMessageBox.confirm(
        `确定要删除应用 "${app.appName}" 吗？删除后相关的用户绑定也会被清除。`,
        '确认删除',
        { type: 'warning' }
      ).then(async () => {
        try {
          const response = await fetch(`/api/apps/${app.id}`, {
            method: 'DELETE'
          })
          const result = await response.json()
          if (result.success) {
            ElMessage.success('删除成功')
            loadApps()
          } else {
            ElMessage.error(result.message || '删除失败')
          }
        } catch (error) {
          console.error('Delete failed:', error)
          ElMessage.error('删除失败')
        }
      }).catch(() => {})
    }

    const saveUserBinding = async () => {
      if (!currentApp.value) return
      
      bindSaving.value = true
      try {
        const response = await fetch(`/api/apps/${currentApp.value.appCode}/users`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ userIds: selectedUserIds.value })
        })
        
        const result = await response.json()
        if (result.success) {
          ElMessage.success('绑定成功')
          userBindDialogVisible.value = false
        } else {
          ElMessage.error(result.message || '绑定失败')
        }
      } catch (error) {
        console.error('Save binding failed:', error)
        ElMessage.error('绑定失败')
      } finally {
        bindSaving.value = false
      }
    }

    onMounted(() => {
      loadApps()
      loadAllUsers()
    })

    return {
      loading,
      submitting,
      bindSaving,
      apps,
      allUsers,
      dialogVisible,
      userBindDialogVisible,
      isEdit,
      formRef,
      form,
      rules,
      currentApp,
      selectedUserIds,
      formatDate,
      showAddDialog,
      showEditDialog,
      showUserBindDialog,
      submitForm,
      confirmDelete,
      saveUserBinding
    }
  }
}
</script>

<style scoped>
.app-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.bind-dialog-content {
  padding: 10px 0;
}

.bind-info {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

:deep(.el-transfer) {
  display: flex;
  justify-content: center;
}

:deep(.el-transfer-panel) {
  width: 220px;
}
</style>
