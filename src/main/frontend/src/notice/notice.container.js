import NoticeUI from "./notice.presenter";
import { useState } from "react";
export default function Notice(props) {
  const [color, setColor] = useState(props.el.checked ? "white" : "orange");
  const onClickNotice = () => {
    setColor("white");
    props.router.push("/wms");
    //라우팅기능+updateNoticeTable아니면 닫앗을때 한꺼번에 처리하는 방법쓸까...
  };
  return <NoticeUI el={props.el} color={color} onClickNotice={onClickNotice} />;
}
