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
                <el-button type="primary" @click="this.AddBookVisible = true " style="width: 120px; margin-right: 40px; ">图书入库</el-button>
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
                <el-button type="primary" @click="QueryBookVisible = true" style="width: 120px;">查询图书</el-button>
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
                <el-input v-model="toAddBook" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书名称：
                <el-input v-model="toAddBook" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书出版社：
                <el-input v-model="toAddBook" style="width: 14vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书年份：
                <el-input v-model="toAddBook" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书作者：
                <el-input v-model="toAddBook" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书价格：
                <el-input v-model="toAddBook" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书库存：
                <el-input v-model="toAddBook" style="width: 15vw;" clearable />
            </div>
            <template #footer>
                <span>
                    <el-button @click="AddBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmAddBook"
                               :disabled="toAddBook.length === 0">确定</el-button>
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
                <el-input v-model="toQueryBook" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书名称：
                <el-input v-model="toQueryBook" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书出版社：
                <el-input v-model="toQueryBook" style="width: 14vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书年份：
                <span>
                    <el-input v-model="toQueryBook" style="width: 7vw;" clearable />
                    -
                    <el-input v-model="toQueryBook" style="width: 7vw;" clearable />
                </span>
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
              图书作者：
              <el-input v-model="toQueryBook" style="width: 15vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书价格：
                <span>
                    <el-input v-model="toQueryBook" style="width: 7vw;" clearable />
                    -
                    <el-input v-model="toQueryBook" style="width: 7vw;" clearable />
                </span>
            </div>
            <template #footer>
                <span>
                    <el-button @click="QueryBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmQueryBook"
                               :disabled="toQueryBook.length === 0">确定</el-button>
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
            AddBookVisible: false, // 添加图书对话框
            AddBatchVisible: false, // 批量添加图书对话框
            ModifyBookVisible: false, // 修改图书对话框
            DeleteBookVisible: false, // 删除图书对话框
            BorrowBookVisible: false, // 借阅图书对话框
            ReturnBookVisible: false, // 归还图书对话框
            QueryBookVisible: false, // 查询图书对话框
            toAddBook: '', // 添加图书内容
            toAddBatch: '', // 批量添加图书内容
            toModifyBook: '', // 修改图书内容
            toDeleteBook: '', // 删除图书内容
            toBorrowBook: '', // 借阅图书内容
            toReturnBook: '', // 归还图书内容
            toQueryBook: '', // 查询图书内容

        }
    },
    methods: {
        ConfirmAddBook() {
        },
        ConfirmAddBatch(){},

        ConfirmQueryBook() {}
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

.newBookBox {
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