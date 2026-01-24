<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="openDialog()">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>
      
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <el-tag 
              v-for="role in row.roles" 
              :key="role.id" 
              size="small"
              style="margin-right: 4px;"
            >
              {{ role.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- User Dialog -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="500px"
    >
      <el-form 
        ref="formRef"
        :model="form" 
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            :placeholder="isEdit ? '留空表示不修改' : '请输入密码'"
            show-password
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%;">
            <el-option 
              v-for="role in roles" 
              :key="role.id" 
              :label="role.roleName" 
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '../utils/request'

export default {
  name: 'UserManagement',
  components: { Plus },
  setup() {
    const loading = ref(false)
    const submitting = ref(false)
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const users = ref([])
    const roles = ref([])
    const formRef = ref(null)
    const editingId = ref(null)
    
    const form = reactive({
      username: '',
      password: '',
      nickname: '',
      roleIds: [],
      enabled: true
    })
    
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 50, message: '用户名长度必须在3-50之间', trigger: 'blur' }
      ],
      password: [
        { 
          validator: (rule, value, callback) => {
            if (!isEdit.value && !value) {
              callback(new Error('请输入密码'))
            } else if (value && (value.length < 6 || value.length > 100)) {
              callback(new Error('密码长度必须在6-100之间'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ],
      roleIds: [
        { type: 'array', required: true, message: '请选择至少一个角色', trigger: 'change' }
      ]
    }
    
    const fetchUsers = async () => {
      loading.value = true
      try {
        const response = await request.get('/users')
        if (response.success) {
          users.value = response.data
        }
      } catch (error) {
        console.error('Failed to fetch users:', error)
      } finally {
        loading.value = false
      }
    }
    
    const fetchRoles = async () => {
      try {
        const response = await request.get('/roles')
        if (response.success) {
          roles.value = response.data
        }
      } catch (error) {
        console.error('Failed to fetch roles:', error)
      }
    }
    
    const openDialog = (user = null) => {
      isEdit.value = !!user
      editingId.value = user?.id || null
      
      if (user) {
        form.username = user.username
        form.password = ''
        form.nickname = user.nickname || ''
        form.roleIds = user.roleIds || []
        form.enabled = user.enabled
      } else {
        form.username = ''
        form.password = ''
        form.nickname = ''
        form.roleIds = []
        form.enabled = true
      }
      
      dialogVisible.value = true
    }
    
    const handleSubmit = async () => {
      if (!formRef.value) return
      
      try {
        await formRef.value.validate()
        submitting.value = true
        
        const data = { ...form }
        if (isEdit.value && !data.password) {
          delete data.password
        }
        
        let response
        if (isEdit.value) {
          response = await request.put(`/users/${editingId.value}`, data)
        } else {
          response = await request.post('/users', data)
        }
        
        if (response.success) {
          ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
          dialogVisible.value = false
          fetchUsers()
        } else {
          ElMessage.error(response.message || '操作失败')
        }
      } catch (error) {
        if (error !== false) {
          ElMessage.error(error.response?.data?.message || '操作失败')
        }
      } finally {
        submitting.value = false
      }
    }
    
    const handleDelete = async (user) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除用户 "${user.username}" 吗？`,
          '确认删除',
          { type: 'warning' }
        )
        
        const response = await request.delete(`/users/${user.id}`)
        if (response.success) {
          ElMessage.success('删除成功')
          fetchUsers()
        } else {
          ElMessage.error(response.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }
    
    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      const date = new Date(dateStr)
      return date.toLocaleString('zh-CN')
    }
    
    onMounted(() => {
      fetchUsers()
      fetchRoles()
    })
    
    return {
      loading,
      submitting,
      dialogVisible,
      isEdit,
      users,
      roles,
      form,
      rules,
      formRef,
      openDialog,
      handleSubmit,
      handleDelete,
      formatDate
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
