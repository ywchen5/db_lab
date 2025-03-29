<!-- TODO: YOUR CODE HERE -->
<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%; ">
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理</div>
        <p style="margin-left: 40px; margin-top: 50px; font-size: 1.4em; font-weight: bold;">欢迎使用图书管理系统！</p>

        <!-- 图书管理操作 -->
        <div class="plate" style="margin-left: 40px; margin-top: 50px;">
            <p style="margin-left: 10px; margin-top: 10px; font-size: 1.2em; font-weight: bold;">图书管理操作：</p>

            <el-divider />

            <div style="margin-left: 40px; margin-top: 30px; font-size: 1.4em; font-weight: bold;">
                <el-button type="primary" @click="this.AddBookVisible = true, toAddBook.toAddCategory = '', toAddBook.toAddTitle = '',
                toAddBook.toAddPress = '', toAddBook.toAddPublishYear = '', toAddBook.toAddAuthor = '',
                toAddBook.toAddPrice = '', toAddBook.toAddStock = ''" style="width: 120px; margin-right: 40px;">图书入库</el-button>
                <el-button type="primary" @click="toQueryBook" style="width: 120px; margin-right: 40px;">增加库存</el-button>
                <el-button type="primary" @click="toModifyBook" style="width: 120px; margin-right: 40px;">修改图书信息</el-button>
                <el-button type="primary" @click="toDeleteBook" style="width: 120px;">删除图书</el-button>
            </div>`
        </div>

        <!-- 图书借阅与归还 -->
        <div class ="plate" style="margin-left: 40px; margin-top: 50px;">
            <p style="margin-left: 10px; margin-top: 10px; font-size: 1.2em; font-weight: bold;">图书借阅与归还：</p>

            <el-divider />

            <div style="margin-left: 40px; margin-top: 30px; font-size: 1.4em; font-weight: bold;">
                <el-button type="primary" @click="toBorrowBook" style="width: 120px; margin-right: 40px;">借阅书籍</el-button>
                <el-button type="primary" @click="toReturnBook" style="width: 120px;">归还书籍</el-button>
            </div>
        </div>

        <!-- 图书查询 -->
        <div class="plate" style="margin-left: 40px; margin-top: 50px;">
            <p style="margin-left: 10px; margin-top: 10px; font-size: 1.2em; font-weight: bold;">图书查询：</p>

            <el-divider />

            <div style="margin-left: 40px; margin-top: 30px; font-size: 1.4em; font-weight: bold;">
                <el-button type="primary" @click="toQueryBook.toQueryCategory = '', toQueryBook.toQueryTitle = '', toQueryBook.toQueryPress = '',
                toQueryBook.toQueryAuthor = '', toQueryBook.toQueryMinPublishYear = '', toQueryBook.toQueryMaxPublishYear = '',
                toQueryBook.toQueryMinPrice = '', toQueryBook.toQueryMaxPrice = '', this.QueryBookVisible = true" style="width: 120px; margin-right: 40px;">查询图书</el-button>
            </div>
        </div>

        <!-- 单本入库对话框 -->
        <el-dialog v-model="AddBookVisible" width="30%" font-weight="bold" align-center>
            <template #title>
                <span style="font-weight: bold; font-size: 1.4em" >图书入库</span>
            </template>

            <span style="font-size: 1rem; font-weight: bold; margin-left: 8vw;">单本入库
                <el-button type="text" @click="AddBatchVisible = true; AddBookVisible = false" style="margin-left: 4vw; font-weight: bold; font-size: 1rem; ">
                    批量添加
                </el-button>
            </span>

            <el-divider />

            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书类别：
                <el-input v-model="toAddBook.toAddCategory" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书名称：
                <el-input v-model="toAddBook.toAddTitle" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书出版社：
                <el-input v-model="toAddBook.toAddPress" style="width: 14vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书年份：
                <el-input v-model="toAddBook.toAddPublishYear" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书作者：
                <el-input v-model="toAddBook.toAddAuthor" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书价格：
                <el-input v-model="toAddBook.toAddPrice" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书库存：
                <el-input v-model="toAddBook.toAddStock" style="width: 15vw;" clearable />
            </div>
            <template #footer>
                <span>
                    <el-button @click="AddBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmAddBook"
                               :disabled="toAddBook.toAddCategory.length === 0 || toAddBook.toAddTitle.length === 0 ||
                                toAddBook.toAddPress.length === 0 || toAddBook.toAddPublishYear.length === 0 ||
                                toAddBook.toAddAuthor.length === 0 || toAddBook.toAddPrice.length === 0 ||
                                toAddBook.toAddStock.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <! -- 批量入库对话框 -->
        <el-dialog v-model="AddBatchVisible" width="30%" font-weight="bold" align-center>
            <template #title>
                <span style="font-weight: bold; font-size: 1.4em" >图书入库</span>
            </template>

            <el-button type="text" @click="AddBookVisible = true; AddBatchVisible = false" style="margin-left: 8vw; font-weight: bold; font-size: 1rem; ">
                单本入库
            </el-button>
            <span style="font-size: 1rem; font-weight: bold; margin-left: 4vw;">
                批量添加
            </span>

            <template #footer>
                <span>
                    <el-button @click="AddBatchVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmAddBatch"
                               :disabled="toAddBook.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>





        <! -- 查询对话框 -->
        <el-dialog v-model="QueryBookVisible" width="30%" font-weight="bold" align-center>
            <template #title>
                <span style="font-weight: bold; font-size: 1.4em" >图书查询</span>
            </template>

            <el-divider />

            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书类别：
                <el-input v-model="toQueryBook.toQueryCategory" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书名称：
                <el-input v-model="toQueryBook.toQueryTitle" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书出版社：
                <el-input v-model="toQueryBook.toQueryPress" style="width: 14vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书年份：
                <span>
                  <el-input v-model="toQueryBook.toQueryMinPublishYear" style="width: 7vw;" clearable />
                    -
                    <el-input v-model="toQueryBook.toQueryMaxPublishYear" style="width: 7vw;" clearable />
                </span>
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
              图书作者：
              <el-input v-model="toQueryBook.toQueryAuthor" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书价格：
                <span>
                    <el-input v-model="toQueryBook.toQueryMinPrice" style="width: 7vw;" clearable />
                    -
                    <el-input v-model="toQueryBook.toQueryMaxPrice" style="width: 7vw;" clearable />
                </span>
            </div>
            <template #footer>
                <span>
                    <el-button @click="QueryBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmQueryBook">确认</el-button>
                </span>
            </template>
        </el-dialog>

        <! -- 查询结果对话框 -->
        <el-dialog v-model="QueryResultVisible" width="60%" font-weight="bold" align-center>
            <template #title>
                <span style="font-weight: bold; font-size: 1.4em" >查询结果</span>
            </template>

            <el-table :data="tableData" height="600"
                      :default-sort="{ prop: 'bookId', order: 'ascending' }" :table-layout="'auto'"
                      style="width: 90%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
                <el-table-column prop="bookId" label="图书ID" />
                <el-table-column prop="category" label="图书类别" />
                <el-table-column prop="title" label="图书名称" />
                <el-table-column prop="press" label="图书出版社" />
                <el-table-column prop="publishYear" label="图书年份" />
                <el-table-column prop="author" label="图书作者" />
                <el-table-column prop="price" label="图书价格" />
                <el-table-column prop="stock" label="图书库存" />
            </el-table>
            <template #footer>
                <span>
                    <el-button @click="QueryResultVisible = false">关闭</el-button>
                </span>
            </template>
        </el-dialog>

    </el-scrollbar>
</template>




<script>
import { Delete, Edit, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
    data() {
        return {
            Delete,
            Edit,
            Search,
            toSearch: '', // 搜索内容
            tableData: [{ // 列表项
                bookId: 1,
                category: '计算机',
                title: '数据库系统',
                press: '浙江大学出版社',
                publishYear: 2020,
                author: 'sjl',
                price: 99.9,
                stock: 10
            }],

            AddBookVisible: false, // 添加图书对话框
            AddBatchVisible: false, // 批量添加图书对话框
            ModifyBookVisible: false, // 修改图书对话框
            DeleteBookVisible: false, // 删除图书对话框
            BorrowBookVisible: false, // 借阅图书对话框
            ReturnBookVisible: false, // 归还图书对话框
            QueryBookVisible: false, // 查询图书对话框
            QueryResultVisible: false, // 查询结果对话框
            toAddBook: {
                toAddCategory: '',
                toAddTitle: '',
                toAddPress: '',
                toAddPublishYear: '',
                toAddAuthor: '',
                toAddPrice: '',
                toAddStock: ''
            }, // 待添加图书信息
            toAddBatch: '', // 批量添加图书内容
            toModifyBook: '', // 修改图书内容
            toDeleteBook: '', // 删除图书内容
            toBorrowBook: '', // 借阅图书内容
            toReturnBook: '', // 归还图书内容
            toQueryBook: {
                toQueryCategory: '',
                toQueryTitle: '',
                toQueryPress: '',
                toQueryAuthor: '',
                toQueryMinPublishYear: '',
                toQueryMaxPublishYear: '',
                toQueryMinPrice: '',
                toQueryMaxPrice: ''
            } // 查询图书内容
        }
    },
    methods: {
        ConfirmAddBook() {
            axios.post("/book",
                {
                    action: "AddBook",
                    category: this.toAddBook.toAddCategory,
                    title: this.toAddBook.toAddTitle,
                    press: this.toAddBook.toAddPress,
                    publishYear: this.toAddBook.toAddPublishYear,
                    author: this.toAddBook.toAddAuthor,
                    price: this.toAddBook.toAddPrice,
                    stock: this.toAddBook.toAddStock
                })
                .then(response => {
                    ElMessage.success("图书入库成功") // 显示消息提醒
                    this.AddBookVisible = false // 将对话框设置为不可见
                })
        },
        ConfirmAddBatch(){},

        async ConfirmQueryBook() {
            this.tableData = [] // 清空列表
            let response = await axios.get('/book',
            { params: {
                      category: this.toQueryBook.toQueryCategory || undefined,
                      title: this.toQueryBook.toQueryTitle || undefined,
                      press: this.toQueryBook.toQueryPress || undefined,
                      author: this.toQueryBook.toQueryAuthor || undefined,
                      minPublishYear: this.toQueryBook.toQueryMinPublishYear || undefined,
                      maxPublishYear: this.toQueryBook.toQueryMaxPublishYear || undefined,
                      minPrice: this.toQueryBook.toQueryMinPrice || undefined,
                      maxPrice: this.toQueryBook.toQueryMaxPrice || undefined
                    }
                })
            let books = response.data // 获取响应负载
            books.forEach(book => { // 对于每一本书
                this.tableData.push(book) // 将它加入到列表项中
            });
            ElMessage.success("图书查询成功") // 显示消息提醒
            this.QueryBookVisible = false // 将对话框设置为不可见
            this.QueryResultVisible = true // 显示查询结果对话框
        }
    }
}

</script>

<style scoped>
.plate {
    height: 180px;
    width: 750px;
    box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
    margin-top: 40px;
    margin-left: 27.5px;
    margin-right: 10px;
    padding: 7.5px;
    padding-right: 10px;
    padding-top: 15px;
}

.dialogBox {
    height: 300px;
    width: 200px;
    margin-top: 40px;
    margin-left: 27.5px;
    margin-right: 10px;
    padding: 7.5px;
    padding-right: 10px;
    padding-top: 15px;
    box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
    text-align: center;
}

</style>