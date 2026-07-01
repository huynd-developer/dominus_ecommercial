<template>
  <aside class="sidebar-filter">
    <div class="filter-header">
      <h3>BỘ LỌC TÌM KIẾM</h3>
      <svg class="icon-filter" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/><line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/><line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/><line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="17" y1="16" x2="23" y2="16"/></svg>
    </div>

    <div class="filter-group">
      <div class="group-title">Giới tính <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
      <div class="checkbox-list row">
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Nam</label>
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Nữ</label>
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Unisex</label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">Dung tích <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
      <div class="checkbox-list col">
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> 10ml <span class="count">(18)</span></label>
        <label class="custom-checkbox"><input type="checkbox" checked><span class="checkmark"></span> 50ml <span class="count">(42)</span></label>
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> 100ml <span class="count">(27)</span></label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">Nồng độ <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
      <div class="checkbox-list col">
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> EDP <span class="count">(35)</span></label>
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> EDT <span class="count">(20)</span></label>
        <label class="custom-checkbox"><input type="checkbox" checked><span class="checkmark"></span> Parfum <span class="count">(32)</span></label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">Mùi hương <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
      <div class="pill-list">
        <button class="pill-btn">Gỗ</button>
        <button class="pill-btn">Biển</button>
        <button class="pill-btn">Citrus</button>
        <button class="pill-btn">Aromatic</button>
        <button class="pill-btn">Xạ hương</button>
        <button class="pill-btn">Hương thơm sạch</button>
      </div>
    </div>

    <div class="filter-group" style="border-bottom: none;">
      <div class="group-title">Thương hiệu <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
      <div class="checkbox-list col">
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Chanel <span class="count">(28)</span></label>
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Dior <span class="count">(34)</span></label>
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Tom Ford <span class="count">(22)</span></label>
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Creed <span class="count">(18)</span></label>
        <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Maison Francis Kurkdjian <span class="count">(14)</span></label>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar-filter { width: 280px; flex-shrink: 0; background: #fff; padding: 25px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); }
.filter-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eaeaea; padding-bottom: 15px; margin-bottom: 20px; }
.filter-header h3 { font-size: 16px; margin: 0; color: #0a142f; font-weight: 700; }
.icon-filter { width: 20px; height: 20px; color: #c69c6d; }
.filter-group { border-bottom: 1px solid #eaeaea; padding: 15px 0; }
.group-title { display: flex; justify-content: space-between; align-items: center; font-size: 14px; font-weight: 600; margin-bottom: 15px; cursor: pointer; color: #0a142f; }
.chevron { width: 16px; height: 16px; color: #0a142f; }
.checkbox-list { display: flex; gap: 10px; }
.checkbox-list.col { flex-direction: column; }
.checkbox-list.row { justify-content: space-between; }
.custom-checkbox { display: flex; align-items: center; position: relative; padding-left: 28px; cursor: pointer; font-size: 13px; user-select: none; color: #4a5568; }
.custom-checkbox input { position: absolute; opacity: 0; cursor: pointer; height: 0; width: 0; }
.checkmark { position: absolute; top: -2px; left: 0; height: 18px; width: 18px; background-color: white; border: 1px solid #cbd5e0; border-radius: 4px; transition: 0.2s;}
.custom-checkbox:hover input ~ .checkmark { border-color: #0a142f; }
.custom-checkbox input:checked ~ .checkmark { background-color: #0a142f; border-color: #0a142f; }
.checkmark:after { content: ""; position: absolute; display: none; }
.custom-checkbox input:checked ~ .checkmark:after { display: block; left: 6px; top: 2px; width: 4px; height: 9px; border: solid white; border-width: 0 2px 2px 0; transform: rotate(45deg); }
.count { color: #a0aec0; font-size: 12px; margin-left: 4px;}
.pill-list { display: flex; flex-wrap: wrap; gap: 10px; }
.pill-btn { background: white; border: 1px solid #cbd5e0; border-radius: 20px; padding: 6px 15px; font-size: 12px; color: #4a5568; cursor: pointer; transition: 0.2s; }
.pill-btn:hover { border-color: #0a142f; color: #0a142f; }
</style>