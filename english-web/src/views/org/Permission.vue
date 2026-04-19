<template>
  <div class="bg-white p-6 rounded-lg shadow-sm">
    <h2 class="text-xl font-bold text-gray-800 mb-2">权限控制与分配</h2>
    <p class="text-sm text-gray-500 mb-6">勾选您允许本校学生背诵的书籍（由于权限限制，此处列出平台可用书库）</p>

    <div v-loading="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <el-card v-for="book in allBooks" :key="book.id" shadow="hover" class="relative">
        <div class="flex items-start">
          <div class="w-16 h-20 bg-gray-200 rounded overflow-hidden mr-4 flex-shrink-0">
            <img v-if="book.coverUrl" :src="book.coverUrl" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-gray-400">封面</div>
          </div>
          <div class="flex-1">
            <h3 class="font-bold text-gray-800 mb-1 line-clamp-1">{{ book.name }}</h3>
            <p class="text-xs text-gray-500 line-clamp-2">{{ book.description || '暂无简介' }}</p>
          </div>
        </div>
        
        <div class="mt-4 flex justify-between items-center border-t border-gray-100 pt-3">
          <span class="text-sm font-medium" :class="isGranted(book.id) ? 'text-green-600' : 'text-gray-400'">
            {{ isGranted(book.id) ? '已开放' : '未开放' }}
          </span>
          <el-switch 
            :model-value="isGranted(book.id)"
            @change="val => handleToggle(book.id, val)"
            inline-prompt
            active-text="允许"
            inactive-text="禁止"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const allBooks = ref([])
const grantedBookIds = ref(new Set())

async function fetchBooksAndPerms() {
  loading.value = true
  try {
    // If backend doesn't provide allBooks directly to org_admin, we might just list granted ones.
    // Assuming GET /api/books returns all books for mock if needed, or /tenant/books lists granted.
    // In our requirement: "列出平台所有的书，校长可以勾选". We will use /api/books which might return all books in standard.
    const allRes = await request.get('/books') // using standard books endpoint which lists books
    allBooks.value = allRes || []

    const grantedRes = await request.get('/tenant/books') // TenantBooksController listGranted
    if (grantedRes) {
      grantedBookIds.value = new Set(grantedRes.map(b => b.id))
    }
  } catch (err) {
    ElMessage.error(err.message || '获取书库失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchBooksAndPerms())

function isGranted(id) {
  return grantedBookIds.value.has(id)
}

async function handleToggle(bookId, allow) {
  try {
    if (allow) {
      await request.post('/tenant/books/grant', null, { params: { bookId } })
      grantedBookIds.value.add(bookId)
      ElMessage.success('已开放此书')
    } else {
      await request.delete('/tenant/books/revoke', { params: { bookId } })
      grantedBookIds.value.delete(bookId)
      ElMessage.warning('已收回此书权限')
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}
</script>