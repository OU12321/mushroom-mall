import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    // 用户信息: { userId, username, name, role, avatar }
    userInfo: JSON.parse(sessionStorage.getItem('userInfo') || 'null')
  },
  mutations: {
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
      if (userInfo) {
        sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
      } else {
        sessionStorage.removeItem('userInfo')
      }
    }
  },
  actions: {
    login({ commit }, userInfo) {
      commit('SET_USER_INFO', userInfo)
    },
    logout({ commit }) {
      commit('SET_USER_INFO', null)
    }
  },
  getters: {
    role: state => state.userInfo ? state.userInfo.role : null,
    isLogin: state => !!state.userInfo
  }
})
