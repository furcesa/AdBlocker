# 权限说明

## 必需权限

### 1. QUERY_ALL_PACKAGES

**声明**：
```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />

用途：读取已安装应用列表，让用户能为特定应用单独设置广告拦截规则。

Android 版本：

Android 10 及以下：此权限不存在，自动忽略

Android 11+：必须声明才能读取完整应用列表

替代方案：若用户不愿授权，App 会显示空列表，但其他功能正常。

