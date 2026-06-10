import type { Directive } from 'vue';

export const permission: Directive = {
  mounted(el, binding) {
    const { value } = binding; // Lấy mảng role truyền vào: v-permission="['OWNER']"
    
    // 1. Lấy và parse thông tin user an toàn
    const userRawData = localStorage.getItem('currentUser');
    let userRole = 'USER';

    if (userRawData) {
      try {
        const parsedUser = JSON.parse(userRawData);
        
        // Logic dò tìm Role y như bên Router
        let rawRole = '';
        if (parsedUser.role) {
          rawRole = parsedUser.role;
        } else if (Array.isArray(parsedUser.roles) && parsedUser.roles.length > 0) {
          rawRole = parsedUser.roles[0];
        } else if (Array.isArray(parsedUser.authorities) && parsedUser.authorities.length > 0) {
          rawRole = parsedUser.authorities[0].authority || parsedUser.authorities[0];
        }

        if (rawRole && typeof rawRole === 'string') {
          userRole = rawRole.toUpperCase().replace('ROLE_', ''); 
        }
      } catch (e) {
        console.error("Lỗi parse currentUser trong directive:", e);
      }
    }

    // 2. So sánh và ẩn/hiện nút
    if (value && Array.isArray(value) && value.length > 0) {
      const hasPermission = value.includes(userRole);

      // Nếu KHÔNG CÓ QUYỀN -> Bay màu khỏi HTML
      if (!hasPermission) {
        if (el.parentNode) {
          el.parentNode.removeChild(el);
        }
      }
    } else {
      throw new Error(`Cách dùng sai! Phải truyền mảng dạng: v-permission="['OWNER', 'CASHIER']"`);
    }
  },
};