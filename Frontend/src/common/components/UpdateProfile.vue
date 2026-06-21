<template>
  <div class="row justify-content-center">
    <div class="col-lg-9">
      <div class="card shadow-sm border-0 rounded-3">
        <div class="card-body p-4 p-md-5">
          <h4 class="card-title fw-bold text-dark mb-4">Thông tin cá nhân</h4>
          
          <form @submit.prevent="handleUpdateProfile">
            <div class="row g-4">
              <div class="col-md-4 text-center border-end-custom">
                <div class="d-flex flex-column align-items-center justify-content-center h-100 py-2">
                  <div class="position-relative mb-3">
                    <img 
                      :src="avatarPreview" 
                      alt="Avatar" 
                      class="rounded-circle img-thumbnail object-fit-cover shadow-sm"
                      style="width: 140px; height: 140px;"
                    />
                    <label class="position-absolute bottom-0 end-0 bg-primary text-white rounded-circle p-2 cursor-pointer shadow border border-white d-flex align-items-center justify-content-center" style="width: 38px; height: 38px;">
                      <i class="bi bi-camera-fill"></i>
                      <input type="file" class="d-none" accept="image/*" @change="onFileChange" />
                    </label>
                  </div>
                  <p class="text-muted small mb-0 px-2">Định dạng: JPG, PNG.<br>Dung lượng tối đa 2MB.</p>
                </div>
              </div>

              <div class="col-md-8">
                <div class="row g-3">
                  <div class="col-sm-6">
                    <label class="form-label fw-semibold text-secondary small">HỌ VÀ TÊN *</label>
                    <input 
                      type="text" 
                      v-model="customer.fullName"
                      class="form-control" 
                      placeholder="Nhập họ và tên"
                      required
                    />
                  </div>

                  <div class="col-sm-6">
                    <label class="form-label fw-semibold text-muted small">EMAIL (KHÔNG THỂ SỬA)</label>
                    <input 
                      type="email" 
                      v-model="customer.email"
                      class="form-control bg-light text-muted cursor-not-allowed" 
                      disabled 
                    />
                  </div>

                  <div class="col-sm-6">
                    <label class="form-label fw-semibold text-secondary small">SỐ ĐIỆN THOẠI *</label>
                    <input 
                      type="text" 
                      v-model="customer.phoneNumber"
                      class="form-control" 
                      placeholder="Nhập số điện thoại"
                      required
                    />
                  </div>

                  <div class="col-sm-6">
                    <label class="form-label fw-semibold text-secondary small">NGÀY SINH</label>
                    <input 
                      type="date" 
                      v-model="customer.dob"
                      class="form-control" 
                    />
                  </div>

                  <div class="col-12 mt-3">
                    <label class="form-label fw-semibold text-secondary small d-block mb-2">GIỚI TÍNH</label>
                    <div class="d-flex gap-4">
                      <div class="form-check">
                        <input class="form-check-input" type="radio" name="gender" id="male" value="male" v-model="customer.gender">
                        <label class="form-check-label cursor-pointer" for="male">Nam</label>
                      </div>
                      <div class="form-check">
                        <input class="form-check-input" type="radio" name="gender" id="female" value="female" v-model="customer.gender">
                        <label class="form-check-label cursor-pointer" for="female">Nữ</label>
                      </div>
                      <div class="form-check">
                        <input class="form-check-input" type="radio" name="gender" id="other" value="other" v-model="customer.gender">
                        <label class="form-check-label cursor-pointer" for="other">Khác</label>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="text-end mt-4 pt-3 border-top border-light">
                  <button type="submit" class="btn btn-primary px-5 shadow-sm" :disabled="submitting">
                    <span v-if="submitting" class="spinner-border spinner-border-sm me-1"></span>
                    Lưu thay đổi
                  </button>
                </div>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';

const submitting = ref(false);
// Ảnh mặc định lúc đầu của Customer
const avatarPreview = ref('https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80');

const customer = reactive({
  fullName: 'Nguyễn Văn A',
  email: 'nguyenvana@gmail.com',
  phoneNumber: '0987654321',
  dob: '1995-12-25',
  gender: 'male',
  avatarFile: null
});

// Xử lý preview ảnh khi người dùng chọn file mới từ máy tính
const onFileChange = (e) => {
  const file = e.target.files[0];
  if (file) {
    if (file.size > 2 * 1024 * 1024) {
      alert('Kích thước ảnh không được vượt quá 2MB!');
      return;
    }
    customer.avatarFile = file;
    avatarPreview.value = URL.createObjectURL(file); // Tạo chuỗi URL cục bộ để hiển thị ảnh ngay lập tức
  }
};

const handleUpdateProfile = async () => {
  submitting.value = true;
  try {
    // Tương đương với API: PUT /api/v1/customer/profile
    console.log('Gửi API Cập nhật thông tin cá nhân:', customer);
    
    await new Promise(resolve => setTimeout(resolve, 1200));
    alert('Cập nhật thông tin tài khoản thành công!');
  } catch (error) {
    alert('Có lỗi xảy ra khi lưu thông tin.');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}
.object-fit-cover {
  object-fit: cover;
}

/* Thêm đường phân cách mờ ở giữa hai cột khi hiển thị trên màn hình máy tính (md) */
@media (min-width: 768px) {
  .border-end-custom {
    border-right: 1px solid #edf2f7;
  }
}
</style>