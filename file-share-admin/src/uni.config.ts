import { defineConfig } from '@dcloudio/uni-app'

export default defineConfig({
  // 应用基本配置
  pages: [
    {
      path: 'pages/login/login',
      style: {
        navigationBarTitleText: '登录',
        navigationBarBackgroundColor: '#667eea',
        navigationBarTextStyle: 'white'
      }
    },
    {
      path: 'pages/index/index',
      style: {
        navigationBarTitleText: '系统监控',
        navigationBarBackgroundColor: '#667eea',
        navigationBarTextStyle: 'white',
        enablePullDownRefresh: true
      }
    },
    {
      path: 'pages/file/list',
      style: {
        navigationBarTitleText: '文件管理',
        navigationBarBackgroundColor: '#667eea',
        navigationBarTextStyle: 'white'
      }
    },
    {
      path: 'pages/share/config',
      style: {
        navigationBarTitleText: '分享配置',
        navigationBarBackgroundColor: '#667eea',
        navigationBarTextStyle: 'white'
      }
    }
  ],

  // 全局样式
  globalStyle: {
    navigationBarTextStyle: 'black',
    navigationBarTitleText: '',
    navigationBarBackgroundColor: '#F6F6F6',
    backgroundColor: '#FFFFFF',
    pageOrientation: 'portrait'
  },

  // 产品配置
  tabBar: {
    color: '#999999',
    selectedColor: '#667eea',
    borderStyle: 'black',
    backgroundColor: '#ffffff',
    list: [
      {
        pagePath: 'pages/index/index',
        iconPath: 'static/tabbar/home.png',
        selectedIconPath: 'static/tabbar/home-active.png',
        text: '首页'
      },
      {
        pagePath: 'pages/file/list',
        iconPath: 'static/tabbar/file.png',
        selectedIconPath: 'static/tabbar/file-active.png',
        text: '文件'
      }
    ]
  },

  // 预加载
  preloadRule: {
    'pages/index/index': {
      network: 'all',
      packages: ['__UNI__APP']
    }
  },

  // 权限配置
  permission: {
    'scope.camera': {
      desc: '需要使用摄像头'
    },
    'scope.photoLibrary': {
      desc: '需要访问您的相册'
    },
    'scope.writePhotosAlbum': {
      desc: '需要保存图片到您的相册'
    }
  }
})
