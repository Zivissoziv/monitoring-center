<template>
  <div class="role-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="openDialog()">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>
      
      <el-table :data="roles" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" width="120" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="菜单权限" width="300">
          <template #default="{ row }">
            <el-tag 
              v-for="menu in row.menus" 
              :key="menu.id" 
              size="small"
              style="margin: 2px;"
            >
              {{ menu.menuName }}
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
    
    <!-- Role Dialog -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="600px"
    >
      <el-form 
        ref="formRef"
        :model="form" 
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码，如: admin" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称，如: 系统管理员" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="2"
            placeholder="请输入角色描述" 
          />
        </el-form-item>
        <el-form-item label="菜单权限" prop="menuIds">
          <el-checkbox-group v-model="form.menuIds">
            <el-checkbox 
              v-for="menu in menus" 
              :key="menu.id" 
              :label="menu.id"
              style="width: 150px; margin-bottom: 8px;"
            >
              {{ menu.menuName }}
            </el-checkbox>
          </el-checkbox-group>
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
  name: 'RoleManagement',
  components: { Plus },
  setup() {
    const loading = ref(false)
    const submitting = ref(false)
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const roles = ref([])
    const menus = ref([])
    const formRef = ref(null)
    const editingId = ref(null)
    
    const form = reactive({
      roleCode: '',
      roleName: '',
      description: '',
      menuIds: []
    })
    
    const rules = {
      roleCode: [
        { required: true, message: '请输入角色编码', trigger: 'blur' },
        { min: 2, max: 50, message: '角色编码长度必须在2-50之间', trigger: 'blur' }
      ],
      roleName: [
        { required: true, message: '请输入角色名称', trigger: 'blur' },
        { max: 100, message: '角色名称长度不能超过100', trigger: 'blur' }
      ],
      menuIds: [
        { type: 'array', required: true, message: '请选择至少一个菜单', trigger: 'change' }
      ]
    }
    
    const fetchRoles = async () => {
      loading.value = true
      try {
        const response = await request.get('/roles')
        if (response.success) {
          roles.value = response.data
        }
      } catch (error) {
        console.error('Failed to fetch roles:', error)
      } finally {
        loading.value = false
      }
    }
    
    const fetchMenus = async () => {
      try {
        const response = await request.get('/roles/menus')
        if (response.success) {
          menus.value = response.data
        }
      } catch (error) {
        console.error('Failed to fetch menus:', error)
      }
    }
    
    const openDialog = (role = null) => {
      isEdit.value = !!role
      editingId.value = role?.id || null
      
      if (role) {
        form.roleCode = role.roleCode
        form.roleName = role.roleName
        form.description = role.description || ''
        form.menuIds = role.menuIds || []
      } else {
        form.roleCode = ''
        form.roleName = ''
        form.description = ''
        form.menuIds = []
      }
      
      dialogVisible.value = true
    }
    
    const handleSubmit = async () => {
      if (!formRef.value) return
      
      try {
        await formRef.value.validate()
        submitting.value = true
        
        let response
        if (isEdit.value) {
          response = await request.put(`/roles/${editingId.value}`, form)
        } else {
          response = await request.post('/roles', form)
        }
        
        if (response.success) {
          ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
          dialogVisible.value = false
          fetchRoles()
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
    
    const handleDelete = async (role) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除角色 "${role.roleName}" 吗？删除后，该角色下的用户将失去相关权限。`,
          '确认删除',
          { type: 'warning' }
        )
        
        const response = await request.delete(`/roles/${role.id}`)
        if (response.success) {
          ElMessage.success('删除成功')
          fetchRoles()
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
      fetchRoles()
      fetchMenus()
    })
    
    return {
      loading,
      submitting,
      dialogVisible,
      isEdit,
      roles,
      menus,
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
.role-management {
  padding: 20px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
