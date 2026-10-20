const api=require("./api");
(async()=>{
  const r=await api.authenticate(0,0);
  console.log("bootstrap status:",r.status);
  console.log(JSON.stringify(r.data,null,2));
})().catch(e=>console.error("ERR",e.message));
