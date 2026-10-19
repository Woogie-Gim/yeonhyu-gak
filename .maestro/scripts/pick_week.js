// 이번 달 셋째 일요일 칸 id. 일~토 한 줄이 모두 이번 달
function pad(n) { return (n < 10 ? '0' : '') + n }
var now = new Date()
var d = new Date(now.getFullYear(), now.getMonth(), 1)
while (d.getDay() !== 0) d.setDate(d.getDate() + 1)
d.setDate(d.getDate() + 14)
output.startCell = 'cal_cell_' + d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
