import { defineStore } from 'pinia'
import request from '../utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: null,
    username: '',
    nickname: '',
    roles: [],
    menus: [],
    apps: []
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.roles.some(r => r.roleCode === 'admin'),
    menuCodes: (state) => state.menus.map(m => m.menuCode)
  },
  
  actions: {
    async login(username, password) {
      try {
        const response = await request.post('/auth/login', { username, password })
        if (response.success) {
          const data = response.data
          this.token = data.token
          this.userId = data.userId
          this.username = data.username
          this.nickname = data.nickname
          this.roles = data.roles
          this.menus = data.menus
          this.apps = data.apps || []
          
          localStorage.setItem('token', data.token)
          localStorage.setItem('user', JSON.stringify({
            userId: data.userId,
            username: data.username,
            nickname: data.nickname,
            roles: data.roles,
            menus: data.menus,
            apps: data.apps || []
          }))
          
          return { success: true }
        } else {
          return { success: false, message: response.message }
        }
      } catch (error) {
        return { success: false, message: error.response?.data?.message || '登录失败' }
      }
    },
    
    async fetchCurrentUser() {
      try {
        const response = await request.get('/auth/current-user')
        if (response.success) {
          const data = response.data
          this.userId = data.userId
          this.username = data.username
          this.nickname = data.nickname
          this.roles = data.roles
          this.menus = data.menus
          this.apps = data.apps || []
          
          localStorage.setItem('user', JSON.stringify({
            userId: data.userId,
            username: data.username,
            nickname: data.nickname,
            roles: data.roles,
            menus: data.menus,
            apps: data.apps || []
          }))
          
          return true
        }
        return false
      } catch (error) {
        return false
      }
    },
    
    logout() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.nickname = ''
      this.roles = []
      this.menus = []
      this.apps = []
      
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    
    restoreFromStorage() {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          this.userId = user.userId
          this.username = user.username
          this.nickname = user.nickname
          this.roles = user.roles || []
          this.menus = user.menus || []
          this.apps = user.apps || []
        } catch (e) {
          console.error('Failed to parse user from storage', e)
        }
      }
    },
    
    hasMenu(menuCode) {
      return this.menus.some(m => m.menuCode === menuCode)
    }
  }
})
