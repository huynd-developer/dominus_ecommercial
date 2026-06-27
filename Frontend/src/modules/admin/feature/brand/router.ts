export default {
  path: 'brands', // URL sẽ là /admin/brands
  name: 'admin-brand',
  component: () => import('./views/BrandView.vue'),
  meta: {
    title: 'Quản lý Thương hiệu'
  }
};